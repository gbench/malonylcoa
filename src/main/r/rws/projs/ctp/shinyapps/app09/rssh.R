# 极简 R + tcltk SSH 多选终端
strsplit("tcltk,ssh", ",") |> unlist() |> setNames(nm=_) |> 
  lapply(\(pkg) {if(!require("tcltk")) install.packages(pkg); require(pkg,  character.only=T)})

servers <- list(
  "192.168.1.10" = list(user="gbench", pass="123456"),
  "192.168.1.41" = list(user="gbench", pass="123456")
)
sessions <- list()

tt <- tktoplevel()
tktitle(tt) <- "SSH 多选终端"

# 左侧服务器列表（多选）
tkgrid(tklabel(tt, text="选择服务器(Ctrl/Shift多选):"), row=0, column=0, sticky="w", padx=5)
srv_list <- tklistbox(tt, height=6, selectmode="multiple", exportselection=FALSE)
for(s in names(servers)) tkinsert(srv_list, "end", s)
tkselection.set(srv_list, 0)  # 默认选第一个
tkgrid(srv_list, row=1, column=0, rowspan=2, sticky="ns", padx=5, pady=5)

# 右侧命令区
right_frm <- tkframe(tt)
tkgrid(right_frm, row=0, column=1, rowspan=3, sticky="nsew")

tkgrid(tklabel(right_frm, text="命令:"), row=0, column=0, sticky="w")
cmd_var <- tclVar("ls -la; hostname; uptime")
cmd_entry <- tkentry(right_frm, textvariable=cmd_var, width=50)
tkgrid(cmd_entry, row=0, column=1, sticky="we", padx=5)

# 输出区
txt <- tktext(right_frm, width=70, height=20, font="Courier 10")
scr <- tkscrollbar(right_frm, command=function(...) tkyview(txt, ...))
tkconfigure(txt, yscrollcommand=function(...) tkset(scr, ...))
tkgrid(txt, row=1, column=0, columnspan=2, sticky="nsew", pady=5)
tkgrid(scr, row=1, column=2, sticky="ns")
tkgrid.rowconfigure(right_frm, 1, weight=1)
tkgrid.columnconfigure(right_frm, 1, weight=1)

tkgrid.rowconfigure(tt, 1, weight=1)
tkgrid.columnconfigure(tt, 1, weight=1)

# 获取选中的服务器
get_selected <- \() {
  idx <- as.integer(tkcurselection(srv_list))
  if(length(idx) == 0) return(NULL)
  names(servers)[idx + 1]  # Tcl索引从0开始
}

# 执行
run_cmd <- \() {
  targets <- get_selected()
  cmd <- tclvalue(cmd_var)
  
  if(is.null(targets)) {
    tkdelete(txt, "1.0", "end")
    tkinsert(txt, "end", "错误：请至少选择一个服务器\n")
    return()
  }
  
  tkdelete(txt, "1.0", "end")
  tkinsert(txt, "end", paste("执行:", cmd, "\n目标:", paste(targets, collapse=", "), "\n\n"))
  
  for(h in targets) {
    cfg <- servers[[h]]
    tryCatch({
      key <- paste0(cfg$user, "@", h)
      if(is.null(sessions[[key]])) {
        sessions[[key]] <<- ssh_connect(host=key, passwd=cfg$pass)
      }
      r <- ssh_exec_internal(sessions[[key]], cmd)
      out <- rawToChar(r$stdout)
      err <- rawToChar(r$stderr)
      tkinsert(txt, "end", paste0("=== ", h, " ===\n", out))
      if(nchar(err) > 0) tkinsert(txt, "end", paste0("[ERR]", err, "\n"))
      tkinsert(txt, "end", "\n")
    }, error=function(e) {
      tkinsert(txt, "end", paste0("=== ", h, " ===\n错误: ", e$message, "\n\n"))
    })
    tcl("update")
  }
}

# 按钮
btn_frm <- tkframe(right_frm)
tkgrid(btn_frm, row=2, column=0, columnspan=2)

tkbutton(btn_frm, text="执行", command=run_cmd, bg="green") -> btn_run
tkpack(btn_run, side="left", padx=5)

# 删除文本控件内容的命令, tkdelete(txt, "1.0", "end") :第 1 行第 0 列（开头）
tkbutton(btn_frm, text="清空", command=\() tkdelete(txt, "1.0", "end") ) -> btn_clear 
tkpack(btn_clear, side="left", padx=5)

tkbutton(btn_frm, text="全选", command=\() tkselection.set(srv_list, 0, "end")) -> btn_all
tkpack(btn_all, side="left", padx=5)

tkbutton(btn_frm, text="断开", command=\() {
  for(s in sessions) try(ssh_disconnect(s), silent=TRUE)
  sessions <<- list()
  tkinsert(txt, "end", "\n[已断开]\n")
}) -> btn_disc
tkpack(btn_disc, side="left", padx=5)

tkbutton(btn_frm, text="退出", command=\() {
  for(s in sessions) try(ssh_disconnect(s), silent=TRUE)
  tkdestroy(tt)
}, bg="red") -> btn_exit
tkpack(btn_exit, side="left", padx=5)

tkbind(cmd_entry, "<Return>", run_cmd)

tkwait.window(tt)
