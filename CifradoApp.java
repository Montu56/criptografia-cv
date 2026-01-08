import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CifradoApp extends JFrame {

    // Nombres de archivo por defecto para facilitar pruebas
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    public CifradoApp() {
        setTitle("Sistema: RSA (Llaves) + DES (Cifrado) + Firma");
        setSize(500, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Cifrado  y Firma ", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JButton btnKeys = new JButton("1. Generar Llaves RSA (Publica/Privada)");
        JButton btnCipher = new JButton("2. Cifrado/Descifrado (DES + RSA)");
        JButton btnSign = new JButton("3. Firma Digital y Verificacion");

        btnKeys.addActionListener(e -> generarLlavesRSA());
        btnCipher.addActionListener(e -> abrirMenuCifrado());
        btnSign.addActionListener(e -> abrirMenuFirma());

        panel.add(title);
        panel.add(btnKeys);
        panel.add(btnCipher);
        panel.add(btnSign);

        add(panel);
    }

    // --- 1. GENERACIÓN DE LLAVES RSA ---
    private void generarLlavesRSA() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();

            try (FileOutputStream fos = new FileOutputStream(PUBLIC_KEY_FILE)) {
                fos.write(pair.getPublic().getEncoded());
            }
            try (FileOutputStream fos = new FileOutputStream(PRIVATE_KEY_FILE)) {
                fos.write(pair.getPrivate().getEncoded());
            }

            JOptionPane.showMessageDialog(this, 
                "Llaves generadas correctamente:\n" + PUBLIC_KEY_FILE + "\n" + PRIVATE_KEY_FILE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // --- 2. MENU CIFRADO (DES CON ENVOLTURA RSA) ---
    private void abrirMenuCifrado() {
        String[] options = {"Cifrar (Usa Publica)", "Descifrar (Usa Privada)"};
        int choice = JOptionPane.showOptionDialog(this, "Seleccione acción", "Cifrado Hibrido",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) cifrarHibrido();
        else if (choice == 1) descifrarHibrido();
    }

    private void cifrarHibrido() {
        try {
            // 1. Pedir Archivo del Mensaje
            File archivoMensaje = seleccionarArchivo("Seleccione el archivo del MENSAJE a cifrar");
            if (archivoMensaje == null) return;

            // 2. Pedir Llave Pública del Receptor
            File archivoLlavePublica = seleccionarArchivo("Seleccione la LLAVE PÚBLICA del receptor");
            if (archivoLlavePublica == null) return;

            // --- Lógica de Cifrado ---
            
            // A. Generar llave DES efímera (solo para este archivo)
            KeyGenerator keyGen = KeyGenerator.getInstance("DES");
            SecretKey desKey = keyGen.generateKey();

            // B. Cifrar el archivo con DES
            Cipher cipherDes = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipherDes.init(Cipher.ENCRYPT_MODE, desKey);
            byte[] contenido = Files.readAllBytes(archivoMensaje.toPath());
            byte[] contenidoCifrado = cipherDes.doFinal(contenido);

            // C. Cifrar la llave DES con RSA (Usando la Pública)
            byte[] keyBytes = Files.readAllBytes(archivoLlavePublica.toPath());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            Cipher cipherRsa = Cipher.getInstance("RSA");
            cipherRsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] llaveDesCifrada = cipherRsa.doFinal(desKey.getEncoded());

            // D. Guardar resultados
            // Guardamos el mensaje cifrado
            File archivoSalidaMensaje = new File(archivoMensaje.getParent(), archivoMensaje.getName() + ".des");
            try (FileOutputStream fos = new FileOutputStream(archivoSalidaMensaje)) {
                fos.write(contenidoCifrado);
            }

            // Guardamos la llave DES cifrada (necesaria para descifrar)
            File archivoSalidaLlave = new File(archivoMensaje.getParent(), "llave_des_cifrada.key");
            try (FileOutputStream fos = new FileOutputStream(archivoSalidaLlave)) {
                fos.write(llaveDesCifrada);
            }

            JOptionPane.showMessageDialog(this, 
                "Cifrado Exitoso!\n\nSe generaron 2 archivos necesarios para el receptor:\n" +
                "1. Mensaje cifrado: " + archivoSalidaMensaje.getName() + "\n" +
                "2. Llave de sesión cifrada: llave_des_cifrada.key");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al Cifrar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void descifrarHibrido() {
        try {
            // 1. Pedir Mensaje Cifrado (.des)
            File archivoCifrado = seleccionarArchivo("Seleccione el archivo CIFRADO (.des)");
            if (archivoCifrado == null) return;

            // 2. Pedir Llave DES Cifrada (la que se generó al cifrar)
            File archivoLlaveDesCifrada = seleccionarArchivo("Seleccione el archivo de LLAVE DE SESIÓN (llave_des_cifrada.key)");
            if (archivoLlaveDesCifrada == null) return;

            // 3. Pedir Llave Privada del Receptor
            File archivoLlavePrivada = seleccionarArchivo("Seleccione SU LLAVE PRIVADA");
            if (archivoLlavePrivada == null) return;

            // --- Lógica de Descifrado ---

            // A. Recuperar Llave Privada RSA
            byte[] keyBytes = Files.readAllBytes(archivoLlavePrivada.toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(spec);

            // B. Descifrar la llave DES usando RSA
            Cipher cipherRsa = Cipher.getInstance("RSA");
            cipherRsa.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] llaveDesBytesCifrados = Files.readAllBytes(archivoLlaveDesCifrada.toPath());
            byte[] llaveDesBytes = cipherRsa.doFinal(llaveDesBytesCifrados);
            SecretKey desKey = new SecretKeySpec(llaveDesBytes, "DES");

            // C. Descifrar el mensaje usando la llave DES recuperada
            Cipher cipherDes = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipherDes.init(Cipher.DECRYPT_MODE, desKey);
            byte[] contenidoCifrado = Files.readAllBytes(archivoCifrado.toPath());
            byte[] contenidoOriginal = cipherDes.doFinal(contenidoCifrado);

            // D. Guardar
            String nombreOriginal = archivoCifrado.getName().replace(".des", "_descifrado.txt");
            File salida = new File(archivoCifrado.getParent(), nombreOriginal);
            try (FileOutputStream fos = new FileOutputStream(salida)) {
                fos.write(contenidoOriginal);
            }

            JOptionPane.showMessageDialog(this, "¡Descifrado Exitoso!\nArchivo: " + salida.getName());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al Descifrar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- 3. MENU FIRMA (Igual que antes) ---
    private void abrirMenuFirma() {
        String[] options = {"Firmar Documento", "Verificar Firma"};
        int choice = JOptionPane.showOptionDialog(this, "Firma Digital", "Firma RSA",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) firmarDocumento();
        else if (choice == 1) verificarFirma();
    }

    private void firmarDocumento() {
        try {
            File keyFile = new File(PRIVATE_KEY_FILE); // Usa la llave del propio directorio
            if (!keyFile.exists()) {
                JOptionPane.showMessageDialog(this, "Primero genere las llaves (Opción 1).");
                return;
            }
            byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(spec);

            File docFile = seleccionarArchivo("Seleccione archivo para firmar");
            if (docFile == null) return;

            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privateKey);
            
            byte[] content = Files.readAllBytes(docFile.toPath());
            rsa.update(content);
            byte[] firma = rsa.sign();

            File sigFile = new File(docFile.getParent(), docFile.getName() + ".sig");
            try (FileOutputStream fos = new FileOutputStream(sigFile)) {
                fos.write(firma);
            }
            JOptionPane.showMessageDialog(this, "Firma generada: " + sigFile.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void verificarFirma() {
        try {
            File keyFile = new File(PUBLIC_KEY_FILE);
            if (!keyFile.exists()) {
                JOptionPane.showMessageDialog(this, "No se encuentra public.key");
                return;
            }
            byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            File docFile = seleccionarArchivo("Seleccione el archivo ORIGINAL");
            if (docFile == null) return;

            File sigFile = seleccionarArchivo("Seleccione el archivo de FIRMA (.sig)");
            if (sigFile == null) return;

            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(publicKey);
            rsa.update(Files.readAllBytes(docFile.toPath()));

            boolean valido = rsa.verify(Files.readAllBytes(sigFile.toPath()));
            JOptionPane.showMessageDialog(this, valido ? "FIRMA VALIDA!" : "FIRMA INVALIDA!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error verificando: " + e.getMessage());
        }
    }

    // Helper para seleccionar archivos
    private File seleccionarArchivo(String titulo) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(titulo);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CifradoApp().setVisible(true));
    }
}
