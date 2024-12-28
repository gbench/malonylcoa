package gbench.util.data.xls;

import java.util.List;

public class StrMatrix extends DataMatrix<String> {

    public StrMatrix(String[][] cells, List<String> hh) {
        super(cells, hh);
    }

    public StrMatrix(DataMatrix<String> mm) {
        this(mm.data(), mm.keys());
    }

    /**
     * 矩阵转置
     * 
     * @return 矩阵转置
     */
    public StrMatrix transpose() {
        return new StrMatrix(super.transpose());
    }

}
