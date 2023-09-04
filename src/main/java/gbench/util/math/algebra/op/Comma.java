package gbench.util.math.algebra.op;

import java.util.LinkedList;
import java.util.Stack;

import gbench.util.math.algebra.symbol.Node;

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
public class Comma<T, U> extends BinaryOp<T, U> {

    public Comma(T t, U u) {
        super(",", P(t, u));
    }
    
    /**
     * 检测 target 是否是一个 逗号"," 字符串
     * 
     * @param target 待检测的对象
     */
    public static boolean COMMA_TEST(final Object target) {
        if (target == null) {
            return false;
        } // if

        final var _name = target instanceof Node ? ((Node) target).getName() : target.toString();
        return ",".equals(_name.strip());
    }
    
    /**
     * 深度遍历一棵二叉树 <br>
     * deep first flatten 深度优先扁平化 <br>
     * 
     * @param root 二叉树的根节点
     * @return root 进行深度遍历的结果
     */
    public static LinkedList<Node> flatten(final Node root) {
        final var stack = new Stack<Node>(); // 深度遍历的工作堆栈
        final var nodes = new LinkedList<Node>(); // 提取 逗号表达式的参数项目，这是一个树形结构的深度遍历后可以得出参数列表

        if (Comma.COMMA_TEST(root.getName())) { // 只对逗号表达式进行计算
            stack.push(root); // 根节点入栈
            while (!stack.empty()) { // 尝试对root 做深度遍历
                final var node = stack.pop(); // 提取数据节点
                if (Comma.COMMA_TEST(node.getName())) { // 把逗号表达式进行深度遍历
                    final var op = node.getOp(); // 转换成算符结构
                    // 注意这是 先1后2，入栈，出栈的时候就是先2后1
                    op.getArgS().map(Node::PACK).forEach(stack::push); // 参数入栈
                } else { // 提取逗号表达式的节点作为参数项目
                    nodes.addFirst(node); // 由于是先2后1的出栈顺序，因此遍历为了保证初始顺序，这里 采用addFirst 给予恢复。
                } // if
            } // while
        } // if
        
        return nodes;
    }

}
