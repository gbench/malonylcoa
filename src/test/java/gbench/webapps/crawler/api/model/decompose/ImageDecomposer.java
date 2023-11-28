package gbench.webapps.crawler.api.model.decompose;

import gbench.util.data.DataApp.IRecord;
import gbench.webapps.crawler.api.model.SrchModel;
import gbench.webapps.crawler.api.model.analyzer.YuhuanAnalyzer;
import gbench.webapps.crawler.api.model.analyzer.lexer.Trie;

import static gbench.util.data.DataApp.IRecord.REC;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class ImageDecomposer implements IDecomposer {
    
    /**
     * 图片  分词：解构
     * 
     * @param entry 记录需要包含一下字段 <br>
     * - File file 待分词的文件 <br>
     * - YuhuanAnalyzer analyzer  分词器 <br>
     * - ConcurrentLinkedQueue&lt;IRecord&gt;tokens [out] 类型参数  ,tokens的元素是一个IRecord对象需要包括:<br>
     *    &nbsp; &nbsp; &nbsp; &nbsp; symbol:关键词,<br>
     *    &nbsp; &nbsp; &nbsp; &nbsp; statement:上下文语句摘要,  <br>
     *    &nbsp; &nbsp; &nbsp; &nbsp; file:文件名, 以及 type:媒体类型 四个字段  特殊补充字段 <br> 
     *    &nbsp; &nbsp; &nbsp; &nbsp; position 关键词的索引位置:{rownum,sart,end}的IRecord<br>
     *    &nbsp; &nbsp; &nbsp; &nbsp; snapfile 快照文件的位置:{rownum,sart,end}的IRecord<br>
     * - ConcurrentLinkedQueue&lt;IRecord&gt;files[out] 类型参数处理的文件集合 <br>
     */
    @SuppressWarnings("unchecked")
    public void decompose(IRecord entry) {
        
        final File file = (File)entry.get("file");
        final YuhuanAnalyzer yuhuan = (YuhuanAnalyzer)entry.get("analyzer");
        final ConcurrentLinkedQueue<IRecord> tokens = (ConcurrentLinkedQueue<IRecord>)entry.get("tokens");
        final ConcurrentLinkedQueue<File> files = (ConcurrentLinkedQueue<File>)entry.get("files");
        
        if(file==null || !file.exists()) {// file 类型检测
            System.err.println(MessageFormat.format("{0} 为空 或 不存在, 不予处理!",file));
            return;
        }//if
        
        if ( SrchModel.enableImgNameAsKeyword ) {// 开始对图片名称关键词识别功能
            final Trie<String> corpus = yuhuan.findOne(Trie.class);
            final var name = file.getName().split("\\.")[0];// 提取文件名
            
            Stream.concat(//  路径分词
                Stream.of(file.getParentFile()).filter(e->e!=null)// 提取路径信息
                    .flatMap(f->Arrays.stream(f.getPath().split("[:/\\\\]+"))), // 分解路径层级
                Stream.of(name)
            ).forEach(keyword->{
                // 添加关键词
                Trie.addPoints2Trie(corpus, keyword.split(""))
                .addAttribute("category", "word")
                .addAttribute("meaning", keyword);// 记录分词
            });
            
        }// 开启 enableImgNameAsKeyword
        
        // 加入处理列表
        files.add(file);// 记录索引文件
        yuhuan.analyze(file.getAbsolutePath()).stream() // 对文件目录进行分词
        .filter(e->"word".equals(e.get("category")))
        .forEach(rec->{
            final var symbol = rec.str("symbol");
            if(symbol==null)return;
            
            tokens.add(REC(// 加入字段分析结果
                "symbol",symbol, // 关键词
                "statement",file.getAbsolutePath(), //文件语句
                "file",file.getName(), // 文件名称
                "type","image" // 类型
            ));// add
        });// forEach
    }

}
