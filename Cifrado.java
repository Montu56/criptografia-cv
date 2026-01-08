import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class Cifrado extends JFrame {

    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    public Cifrado() {
        setTitle("Programa");
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Cifrado Híbrido y Firma Digital", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));

        JButton btnKeys = new JButton("1. Generar Llaves RSA");
        JButton btnCipher = new JButton("2. Cifrado/Descifrado");
        JButton btnSign = new JButton("3. Firma y Verificación");

        btnKeys.addActionListener(e -> generarLlavesRSA());
        btnCipher.addActionListener(e -> abrirMenuCifrado());
        btnSign.addActionListener(e -> abrirMenuFirma());

        panel.add(title);
        panel.add(btnKeys);
        panel.add(btnCipher);
        panel.add(btnSign);

        add(panel);
    }

    // GENERACON DE LLAVES
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
            JOptionPane.showMessageDialog(this, "Llaves generadas correctamente.");
        } catch (Exception e) {
            mensajeError("Error generando llaves", e);
        }
    }

    // MENU CIFRADO
    private void abrirMenuCifrado() {
        String[] options = {"Cifrar", "Descifrar"};
        int choice = JOptionPane.showOptionDialog(this, "Cifrado Mensajes", "Cifrado",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) cifrarMensaje();
        else if (choice == 1) descifrarMensaje();
    }

    private void cifrarMensaje() {
        try {
            File archivoLlavePublica = seleccionarArchivo("1. Seleccione Llave PÚBLICA Receptor");
            if (archivoLlavePublica == null) return;
            File archivoMensaje = seleccionarArchivo("2. Seleccione MENSAJE (.txt)");
            if (archivoMensaje == null) return;

            KeyGenerator keyGen = KeyGenerator.getInstance("DES");
            SecretKey desKey = keyGen.generateKey();

            Cipher cipherDes = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipherDes.init(Cipher.ENCRYPT_MODE, desKey);
            byte[] mensajeCifrado = cipherDes.doFinal(Files.readAllBytes(archivoMensaje.toPath()));

            byte[] keyBytes = Files.readAllBytes(archivoLlavePublica.toPath());
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
            
            Cipher cipherRsa = Cipher.getInstance("RSA");
            cipherRsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] llaveDesCifrada = cipherRsa.doFinal(desKey.getEncoded());

            // Guardar en un solo archivo
            File salida = new File(archivoMensaje.getParent(), archivoMensaje.getName() + ".cifrado");
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(salida))) {
                dos.writeInt(llaveDesCifrada.length);
                dos.write(llaveDesCifrada);
                dos.write(mensajeCifrado);
            }
            JOptionPane.showMessageDialog(this, "Cifrado listo: " + salida.getName());
        } catch (Exception e) { mensajeError("Error", e); }
    }

    private void descifrarMensaje() {
        try {
            File archivoLlavePrivada = seleccionarArchivo("1. Seleccione SU Llave PRIVADA");
            if (archivoLlavePrivada == null) return;
            File archivoCifrado = seleccionarArchivo("2. Seleccione Archivo .cifrado");
            if (archivoCifrado == null) return;

            byte[] datos = Files.readAllBytes(archivoCifrado.toPath());
            try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(datos))) {
                int lenKey = dis.readInt();
                byte[] keyEnc = new byte[lenKey];
                dis.readFully(keyEnc);
                byte[] msgEnc = new byte[datos.length - 4 - lenKey];
                dis.readFully(msgEnc);

                byte[] keyBytes = Files.readAllBytes(archivoLlavePrivada.toPath());
                PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
                
                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] desKeyBytes = rsa.doFinal(keyEnc);

                Cipher des = Cipher.getInstance("DES/ECB/PKCS5Padding");
                des.init(Cipher.DECRYPT_MODE, new SecretKeySpec(desKeyBytes, "DES"));
                byte[] original = des.doFinal(msgEnc);

                File out = new File(archivoCifrado.getParent(), "DESCIFRADO_" + archivoCifrado.getName().replace(".cifrado", ".txt"));
                Files.write(out.toPath(), original);
                JOptionPane.showMessageDialog(this, "Descifrado: " + out.getName());
            }
        } catch (Exception e) { mensajeError("Error", e); }
    }

    // MENU FIRMA
    private void abrirMenuFirma() {
        String[] options = {"Firmar (Hash -> Cifrado)", "Verificar (Hash vs Inverso)"};
        int choice = JOptionPane.showOptionDialog(this, "Firma Digital", "Firma",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) firmar();
        else if (choice == 1) verificar();
    }

    private void firmar() {
        try {
            //Pedir Archivo Original
            File archivoOriginal = seleccionarArchivo("1. Seleccione el Mensaje Original (.txt)");
            if (archivoOriginal == null) return;

            //Pedir Llave Privada del Emisor
            File archivoLlavePrivada = seleccionarArchivo("2. Seleccione Llave PRIVADA del Emisor");
            if (archivoLlavePrivada == null) return;

            //HASH
            byte[] mensajeBytes = Files.readAllBytes(archivoOriginal.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashDelMensaje = digest.digest(mensajeBytes);

            //CIFRAR HASH CON LLAVE PRIVADA
            // Nota: Usamos Cipher RSA (Encriptar) con la Privada. 
            // Esto permite que la Pública lo pueda "descifrar" después.
            byte[] keyBytes = Files.readAllBytes(archivoLlavePrivada.toPath());
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] hashCifrado = cipher.doFinal(hashDelMensaje);

            // GUARDAR SOLO EL HASH CIFRADO
            File archivoFirma = new File(archivoOriginal.getParent(), "FIRMA_hash_cifrado.bin");
            try (FileOutputStream fos = new FileOutputStream(archivoFirma)) {
                fos.write(hashCifrado);
            }

            JOptionPane.showMessageDialog(this, "Firma generada.\nSe guardó el hash cifrado en: " + archivoFirma.getName());

        } catch (Exception e) {
            mensajeError("Error al firmar", e);
        }
    }

    private void verificar() {
        try {
            //Pedir Mensaje Original
            File archivoOriginal = seleccionarArchivo("1. Seleccione el Texto ORIGINAL (.txt)");
            if (archivoOriginal == null) return;

            //Pedir Hash Cifrado
            File archivoFirma = seleccionarArchivo("2. Seleccione el archivo de FIRMA (Hash Cifrado)");
            if (archivoFirma == null) return;

            //Pedir Llave Pública del Emisor
            File archivoLlavePublica = seleccionarArchivo("3. Seleccione la Llave PÚBLICA del Emisor");
            if (archivoLlavePublica == null) return;

            //HASH DEL TEXTO
            byte[] mensajeBytes = Files.readAllBytes(archivoOriginal.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashCalculado = digest.digest(mensajeBytes);

            
            byte[] keyBytes = Files.readAllBytes(archivoLlavePublica.toPath());
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey); // Inverso del cifrado con privada
            
            byte[] hashCifradoBytes = Files.readAllBytes(archivoFirma.toPath());
            byte[] hashDescifrado = cipher.doFinal(hashCifradoBytes);

            //COMPARACIÓN 
            if (Arrays.equals(hashCalculado, hashDescifrado)) {
                JOptionPane.showMessageDialog(this, "¡VERIFICACIÓN CORRECTA!\nEl texto es auténtico y la firma corresponde.");
            } else {
                JOptionPane.showMessageDialog(this, "¡ERROR!\nEl hash no coincide. El texto fue modificado o la llave no es la correcta.");
            }

        } catch (Exception e) {
            mensajeError("Error de Verificación: La firma no coincide con la llave pública.", e);
        }
    }

    private File seleccionarArchivo(String titulo) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(titulo);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    private void mensajeError(String msg, Exception e) {
        JOptionPane.showMessageDialog(this, msg + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cifrado().setVisible(true));
    }
}