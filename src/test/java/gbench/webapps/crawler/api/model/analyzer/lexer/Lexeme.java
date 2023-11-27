package gbench.webapps.crawler.api.model.analyzer.lexer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 语料词素
 * 符号 --> 意义
 * @author XUQINGHUA
 *
 */
public class Lexeme {
    /**
     * 构造函数
     * @param word 单词
     * @param category 类别
     * @param meaning 词义解释
     */
    public Lexeme(String word,String category,String meaning ){
        this.symbol = word;
        this.category = category;
        this.meaning = meaning;
    }//
    
    /**
     * 重写 hashCode 方法一定要和equals 相匹配
     * 以便可以将该对象用作Map的键值
     */
    public int hashCode(){
        return this.symbol.hashCode();
    }
    
    /**
     * 重写 hashCode 方法一定要和equals 相匹配
     * 以便可以将该对象用作Map的键值
     */
    public boolean equals(Object obj) {
           if(this.hashCode() == obj.hashCode())return true;
           return false;
     }
    
    /**
     * 用于字符串的连接是的格式转换
     */
    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.symbol+":"+this.meaning);
        return buffer.toString();
    }
    
    public String getSymbol() {
        return symbol;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getCategory() {
        return category;
    }
    
    /**
     * 添加词汇标签
     * @param tags 标签集合
     * @return tags 标签集合
     */
    public Lexeme addTags(final String ... tags) {
        Arrays.stream(tags).forEach(tag->{
            this.tags.add(tag);
        });
        return this;
    }
    
    /**
     * 移除词汇标签
     * @param tag 标签名称
     * @return 本对象以提供链式编程
     */
    public Lexeme removeTag(final String tag) {
        tags.remove(tag);
        return this;
    }
    
    /**
     * 清除所有词汇标签
     * @param tag 标签名称
     * @return 本对象以提供链式编程
     */
    public Lexeme clearTag(final String tag) {
        tags.clear();
        return this;
    }
    
    /**
     * 获取标签集合
     * @return 标签集合
     */
    public Set<String> getTags() {
        return this.tags;
    }
    
    /**
     * 添加节点属性
     * 
     * @param objs 节点的属性集合
     * @reurn Lexeme 元素本身，以方便实现链式编程
     */
    public Lexeme addAttributes(final Object... objs) {
        int n = objs.length;
        for (int i = 0; i < n - 1; i += 2) {
            this.attributes.put(objs[i] + "", objs[i + 1]);
        }
        return this;
    }
    
    /**
     * 添加节点属性
     * 
     * @param attributes 属性集合
     * @reurn Lexeme 元素本身，以方便实现链式编程
     */
    public Lexeme addAttributes(final Map<?,?> attributes) {
        attributes.forEach((k,v)->this.addAttributes(k,v));
        return this;
    }
    
    /**
     * 获取属性值
     * @param name 属性名
     * @return 获取属性值
     */
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
    
    /**
     * 获取节点属性
     * @return 节点属性集合
     */
    public Map<String,Object> getAttributes(){
        return this.attributes;
    }
    
    /**
     * 移除属性值
     * @param name 属性名
     * @return 移除的属性的值
     */
    public Object removeAttribute(String name) {
        return this.attributes.remove(name);
    }
    
    private final Set<String> tags = new HashSet<>();
    private final Map<String,Object> attributes = new HashMap<>();

    private final String symbol;// 拼写形式
    private final String meaning;// 单词意义
    private final String category;// 类别
}
