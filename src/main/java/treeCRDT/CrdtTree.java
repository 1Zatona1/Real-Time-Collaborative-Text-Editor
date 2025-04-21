package treeCRDT;

public class CrdtTree {
    private CrdtNode root;
    private int count;

    public CrdtTree() {
        this.count = 0;
    }

    public CrdtNode getRoot() {
        return root;
    }

    public void setRoot(CrdtNode root) {
        this.root = root;
    }

    public int getCount() {
        return count;
    }

}
