import com.articulate.sigma.*;

public class MySigma {

    public static void main(String[] args) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
        System.out.println(kb.instances("PrimaryColor"));
    }
}
