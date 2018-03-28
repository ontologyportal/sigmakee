import com.articulate.sigma.*;

public class MySigma {

    public static void main(String[] args) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println(kb.instances("PrimaryColor"));
    }
}
