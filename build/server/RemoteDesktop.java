import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Function 8: Remote Desktop (dieu khien chuot va ban phim tu xa)
 * Su dung java.awt.Robot de inject su kien vao may chu.
 * Client gui toa do da duoc tinh toan (scale tu kich thuoc panel -> kich thuoc man hinh thuc).
 */
public class RemoteDesktop {

    private static Robot robot;

    static {
        try {
            robot = new Robot();
            robot.setAutoDelay(10);
        } catch (AWTException e) {
            robot = null;
        }
    }

    // ── Lay kich thuoc man hinh ──────────────────────────────────────────────
    public static void sendScreenSize(DataOutputStream out) throws IOException {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        send(out, "OK", d.width + "x" + d.height);
    }

    // ── Chuot ────────────────────────────────────────────────────────────────
    public static void clickMouse(DataOutputStream out, int x, int y, int button) throws IOException {
        if (robot == null) { send(out, "ERROR", "Robot khong kha dung tren server nay"); return; }
        try {
            int mask = (button == 3) ? InputEvent.BUTTON3_DOWN_MASK
                     : (button == 2) ? InputEvent.BUTTON2_DOWN_MASK
                     :                 InputEvent.BUTTON1_DOWN_MASK;
            robot.mouseMove(x, y);
            robot.mousePress(mask);
            robot.mouseRelease(mask);
            send(out, "OK", "Click tai (" + x + "," + y + ") btn=" + button);
        } catch (Exception e) {
            send(out, "ERROR", "Loi click: " + e.getMessage());
        }
    }

    public static void moveMouse(DataOutputStream out, int x, int y) throws IOException {
        if (robot == null) { send(out, "ERROR", "Robot khong kha dung"); return; }
        robot.mouseMove(x, y);
        send(out, "OK", "Move (" + x + "," + y + ")");
    }

    public static void scrollMouse(DataOutputStream out, int x, int y, int amount) throws IOException {
        if (robot == null) { send(out, "ERROR", "Robot khong kha dung"); return; }
        robot.mouseMove(x, y);
        robot.mouseWheel(amount);
        send(out, "OK", "Scroll " + amount + " tai (" + x + "," + y + ")");
    }

    // ── Ban phim ─────────────────────────────────────────────────────────────
    public static void pressKey(DataOutputStream out, int keyCode) throws IOException {
        if (robot == null) { send(out, "ERROR", "Robot khong kha dung"); return; }
        try {
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
            send(out, "OK", "KeyPress: " + keyCode);
        } catch (IllegalArgumentException e) {
            send(out, "ERROR", "Phim khong hop le: " + keyCode);
        }
    }

    public static void typeText(DataOutputStream out, String text) throws IOException {
        if (robot == null) { send(out, "ERROR", "Robot khong kha dung"); return; }
        try {
            for (char c : text.toCharArray()) {
                typeChar(c);
            }
            // Them ENTER o cuoi neu ky tu cuoi la '\n'
            send(out, "OK", "Da nhap: " + text);
        } catch (Exception e) {
            send(out, "ERROR", "Loi nhap van ban: " + e.getMessage());
        }
    }

    // ── Helper: nhap 1 ky tu ─────────────────────────────────────────────────
    private static void typeChar(char c) {
        boolean shift = Character.isUpperCase(c)
            || "!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;

        int code = charToKeyCode(c);
        if (code == -1) return;

        if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(code);
        robot.keyRelease(code);
        if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    private static int charToKeyCode(char c) {
        char lo = Character.toLowerCase(c);
        if (lo >= 'a' && lo <= 'z') return KeyEvent.VK_A + (lo - 'a');
        if (c  >= '0' && c  <= '9') return KeyEvent.VK_0 + (c  - '0');
        switch (c) {
            case ' ':  return KeyEvent.VK_SPACE;
            case '\n': return KeyEvent.VK_ENTER;
            case '\t': return KeyEvent.VK_TAB;
            case '.':  case '>': return KeyEvent.VK_PERIOD;
            case ',':  case '<': return KeyEvent.VK_COMMA;
            case '-':  case '_': return KeyEvent.VK_MINUS;
            case '=':  case '+': return KeyEvent.VK_EQUALS;
            case ';':  case ':': return KeyEvent.VK_SEMICOLON;
            case '/':  case '?': return KeyEvent.VK_SLASH;
            case '\\': case '|': return KeyEvent.VK_BACK_SLASH;
            case '[':  case '{': return KeyEvent.VK_OPEN_BRACKET;
            case ']':  case '}': return KeyEvent.VK_CLOSE_BRACKET;
            case '`':  case '~': return KeyEvent.VK_BACK_QUOTE;
            case '\'': case '"': return KeyEvent.VK_QUOTE;
            case '!':  return KeyEvent.VK_1;
            case '@':  return KeyEvent.VK_2;
            case '#':  return KeyEvent.VK_3;
            case '$':  return KeyEvent.VK_4;
            case '%':  return KeyEvent.VK_5;
            case '^':  return KeyEvent.VK_6;
            case '&':  return KeyEvent.VK_7;
            case '*':  return KeyEvent.VK_8;
            case '(':  return KeyEvent.VK_9;
            case ')':  return KeyEvent.VK_0;
            default:   return -1;
        }
    }

    private static void send(DataOutputStream out, String status, String data) throws IOException {
        out.writeUTF(JsonUtil.textResponse(status, data));
        out.flush();
    }
}
