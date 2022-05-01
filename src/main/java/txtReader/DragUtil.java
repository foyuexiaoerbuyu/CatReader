package txtReader;

import javafx.scene.Node;
import javafx.stage.Stage;

/**
 * 拖动工具类
 * @author Light
 */
public class DragUtil {
    public static void addDragListener(Stage stage, Node root) {
        new DragListener(stage).enableDrag(root);
    }
}
