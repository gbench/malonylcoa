;;
;; 在线帮助文档 http://www.lispworks.com/documentation/HyperSpec/Front/index.htm
;;

;; 计算阶乘函数
(defun factorial (x) (if (< x 1) 1 (* x (factorial (- x 1)))))

;; out put
(format t "the factorial of f 1000 is ~d~%" (factorial 1000))

;; 读取指定文件
(defun read-file-lines (filename)
  (with-open-file (stream filename :direction :input)
    (loop for line = (read-line stream nil nil)
          while line
          collect line)))

;; 定义一个函数，根据关键字返回对应的类型说明符
(defun keyword-to-type (keyword)
  (case keyword
    (:uint8 '(unsigned-byte 8)) ;; 单字节 
    (:uint16 '(unsigned-byte 16)) ;; 双字节
    (otherwise keyword))) ;; keyword本身

;; 使用示例
(let ((element-type (keyword-to-type :uint8)))
  (with-open-file (stream "hello.lisp" :direction :input :element-type element-type) ;; 打开文件
    (let ((byte (read-byte stream nil nil)))
      (when byte
        (format t "Read byte: ~a~%" byte)))))


;; 使用示例
(let ((file-content (read-file-lines "hello.lisp")))
  (dolist (line file-content)
    (format t "~a~%" line)))
