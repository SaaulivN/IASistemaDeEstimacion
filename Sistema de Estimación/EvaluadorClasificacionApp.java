import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class EvaluadorClasificacionApp extends JFrame {
    private JTextField txtRutaArchivo, txtNumClases, txtNombreSalidaFinal;
    private JButton btnCargar, btnPasoAPaso, btnProcesarRestante, btnTerminar, btnModoCompleto;
    private JTextArea txtAreaResultados;
    private JTable tablaMatriz;
    private DefaultTableModel modeloTablaMatriz;

    private List<RegistroDato> dataset = new ArrayList<>();
    private List<String> clasesUnicas = new ArrayList<>();
    private int indiceActual = 0;
    private boolean modoPasoActivo = false;

    public EvaluadorClasificacionApp() {
        setTitle("Sistema de Evaluación de Modelos de Clasificación");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel panelConfig = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panelConfig.add(new JLabel("Archivo CSV (Ruta):"), gbc);
        txtRutaArchivo = new JTextField(25);
        gbc.gridx = 1; panelConfig.add(txtRutaArchivo, gbc);
        btnCargar = new JButton("Examinar / Cargar");
        gbc.gridx = 2; panelConfig.add(btnCargar, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelConfig.add(new JLabel("Cantidad de Clases:"), gbc);
        txtNumClases = new JTextField("3", 5); // Valor por defecto 3
        gbc.gridx = 1; panelConfig.add(txtNumClases, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panelConfig.add(new JLabel("Nombre Archivo Final CSV:"), gbc);
        txtNombreSalidaFinal = new JTextField("ResultadosFinal.csv", 15);
        gbc.gridx = 1; panelConfig.add(txtNombreSalidaFinal, gbc);

        add(panelConfig, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 10, 10));
        
        txtAreaResultados = new JTextArea();
        txtAreaResultados.setEditable(false);
        JScrollPane scrollTexto = new JScrollPane(txtAreaResultados);
        scrollTexto.setBorder(BorderFactory.createTitledBorder("Resultados y Evaluación Paso a Paso"));
        panelCentro.add(scrollTexto);

        modeloTablaMatriz = new DefaultTableModel();
        tablaMatriz = new JTable(modeloTablaMatriz);
        JScrollPane scrollTabla = new JScrollPane(tablaMatriz);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Matriz de Confusión"));
        panelCentro.add(scrollTabla);

        add(panelCentro, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPasoAPaso = new JButton("1. Iniciar Paso a Paso (Siguiente)");
        btnProcesarRestante = new JButton("Procesar Restante");
        btnTerminar = new JButton("Terminar Proceso");
        btnModoCompleto = new JButton("2. Procesar Dataset Completo");

        btnPasoAPaso.setEnabled(false);
        btnProcesarRestante.setEnabled(false);
        btnTerminar.setEnabled(false);
        btnModoCompleto.setEnabled(false);

        panelBotones.add(btnPasoAPaso);
        panelBotones.add(btnProcesarRestante);
        panelBotones.add(btnTerminar);
        panelBotones.add(btnModoCompleto);

        add(panelBotones, BorderLayout.SOUTH);

        btnCargar.addActionListener(e -> cargarArchivoCSV());
        btnPasoAPaso.addActionListener(e -> ejecutarPasoAPaso());
        btnProcesarRestante.addActionListener(e -> procesarRestanteDataset());
        btnTerminar.addActionListener(e -> reiniciarProceso());
        btnModoCompleto.addActionListener(e -> ejecutarModoCompleto());
    }

    private void cargarArchivoCSV() {
        JFileChooser fileChooser = new JFileChooser();
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            txtRutaArchivo.setText(archivo.getAbsolutePath());
            try {
                dataset.clear();
                clasesUnicas.clear();
                BufferedReader br = new BufferedReader(new FileReader(archivo));
                String linea;
                boolean esPrimeraLinea = true;

                while ((linea = br.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty()) continue; // Ignorar líneas vacías

                    // Omitir la cabecera (ID-Dato, Clase verdadera, etc.)
                    if (esPrimeraLinea) {
                        esPrimeraLinea = false;
                        // Validar si la primera línea es texto de cabecera o datos
                        if (linea.toLowerCase().contains("id") || linea.toLowerCase().contains("clase")) {
                            continue;
                        }
                    }

                    String[] partes = linea.split(",");
                    if (partes.length >= 3) {
                        String id = partes[0].trim();
                        String real = partes[1].trim();
                        String pred = partes[2].trim();
                        dataset.add(new RegistroDato(id, real, pred));
                        if (!clasesUnicas.contains(real)) clasesUnicas.add(real);
                        if (!clasesUnicas.contains(pred)) clasesUnicas.add(pred);
                    }
                }
                br.close();
                Collections.sort(clasesUnicas);
                
                txtAreaResultados.setText("Archivo cargado exitosamente.\nRegistros leídos: " + dataset.size() + "\nClases detectadas: " + clasesUnicas + "\n");
                btnPasoAPaso.setEnabled(true);
                btnModoCompleto.setEnabled(true);
                indiceActual = 0;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void ejecutarPasoAPaso() {
        if (!modoPasoActivo) {
            modoPasoActivo = true;
            indiceActual = 0;
            txtAreaResultados.setText("--- INICIO MODO PASO A PASO ---\n");
            btnModoCompleto.setEnabled(false);
            btnProcesarRestante.setEnabled(true);
            btnTerminar.setEnabled(true);
        }

        if (indiceActual < dataset.size()) {
            RegistroDato dato = dataset.get(indiceActual);
            txtAreaResultados.append("Dato ID: " + dato.id + " | Real: " + dato.real + " | Predicción: " + dato.prediccion + "\n");
            indiceActual++;
            if (indiceActual >= dataset.size()) {
                txtAreaResultados.append("¡Se han recorrido todos los datos de prueba!\n");
                btnPasoAPaso.setEnabled(false);
                btnProcesarRestante.setEnabled(false);
                finalizarYGuardarResultados();
            }
        }
    }

    private void procesarRestanteDataset() {
        while (indiceActual < dataset.size()) {
            RegistroDato dato = dataset.get(indiceActual);
            txtAreaResultados.append("[Auto] ID: " + dato.id + " | Real: " + dato.real + " | Pred: " + dato.prediccion + "\n");
            indiceActual++;
        }
        btnPasoAPaso.setEnabled(false);
        btnProcesarRestante.setEnabled(false);
        finalizarYGuardarResultados();
    }

    private void ejecutarModoCompleto() {
        txtAreaResultados.setText("--- EJECUTANDO MODO COMPLETO (TODO EL DATASET) ---\n");
        for (RegistroDato dato : dataset) {
            txtAreaResultados.append("ID: " + dato.id + " | Real: " + dato.real + " | Pred: " + dato.prediccion + "\n");
        }
        finalizarYGuardarResultados();
    }

    private void finalizarYGuardarResultados() {
        try {
            int n = clasesUnicas.size();
            int[][] matrizConfusion = new int[n][n];

            for (RegistroDato dato : dataset) {
                int rIdx = clasesUnicas.indexOf(dato.real);
                int pIdx = clasesUnicas.indexOf(dato.prediccion);
                if (rIdx != -1 && pIdx != -1) {
                    matrizConfusion[rIdx][pIdx]++;
                }
            }

            String[] columnas = new String[n + 1];
            columnas[0] = "Real \\ Pred";
            for (int i = 0; i < n; i++) columnas[i + 1] = clasesUnicas.get(i);

            Object[][] datosTabla = new Object[n][n + 1];
            for (int i = 0; i < n; i++) {
                datosTabla[i][0] = clasesUnicas.get(i);
                for (int j = 0; j < n; j++) {
                    datosTabla[i][j + 1] = matrizConfusion[i][j];
                }
            }
            modeloTablaMatriz.setDataVector(datosTabla, columnas);

            for (int c = 0; c < n; c++) {
                String claseActual = clasesUnicas.get(c);
                String nombreArchClase = "Evaluacion_Clase_" + claseActual + ".csv";
                PrintWriter pw = new PrintWriter(new File(nombreArchClase));
                pw.println("ID Dato,Evaluacion (TP/TN/FP/FN)");
                for (RegistroDato dato : dataset) {
                    String eval = "";
                    boolean esReal = dato.real.equals(claseActual);
                    boolean esPred = dato.prediccion.equals(claseActual);
                    if (esReal && esPred) eval = "TP";
                    else if (!esReal && esPred) eval = "FP";
                    else if (esReal && !esPred) eval = "FN";
                    else eval = "TN";
                    pw.println(dato.id + "," + eval);
                }
                pw.close();
            }

            double sumaPrecision = 0, sumaRecall = 0, sumaAccuracy = 0, sumaFScore = 0;
            String nombreArchivoFinal = txtNombreSalidaFinal.getText().trim();
            if (nombreArchivoFinal.isEmpty()) nombreArchivoFinal = "ResultadosFinal.csv";

            PrintWriter pwFinal = new PrintWriter(new File(nombreArchivoFinal));
            pwFinal.println("Clase,TP,FP,TN,FN,Precision,Recall,Accuracy,F-Score");

            txtAreaResultados.append("\n=== MÉTRICAS FINALES ESTIMADAS ===\n");
            int totalDatos = dataset.size();

            for (int i = 0; i < n; i++) {
                int tp = matrizConfusion[i][i];
                int fp = 0, fn = 0, tn = 0;

                for (int j = 0; j < n; j++) {
                    if (j != i) {
                        fp += matrizConfusion[j][i];
                        fn += matrizConfusion[i][j];
                    }
                }

                for (int r = 0; r < n; r++) {
                    for (int c = 0; c < n; c++) {
                        if (r != i && c != i) {
                            tn += matrizConfusion[r][c];
                        }
                    }
                }

                double precision = (tp + fp) == 0 ? 0 : (double) tp / (tp + fp);
                double recall = (tp + fn) == 0 ? 0 : (double) tp / (tp + fn);
                double accuracy = totalDatos == 0 ? 0 : (double) (tp + tn) / totalDatos;
                double fscore = (precision + recall) == 0 ? 0 : 2 * (precision * recall) / (precision + recall);

                sumaPrecision += precision;
                sumaRecall += recall;
                sumaAccuracy += accuracy;
                sumaFScore += fscore;

                pwFinal.println(clasesUnicas.get(i) + "," + tp + "," + fp + "," + tn + "," + fn + "," + 
                                precision + "," + recall + "," + accuracy + "," + fscore);

                txtAreaResultados.append(String.format("Clase %s -> TP:%d, FP:%d, TN:%d, FN:%d | Prec: %.2f, Rec: %.2f, Acc: %.2f, F1: %.2f\n",
                        clasesUnicas.get(i), tp, fp, tn, fn, precision, recall, accuracy, fscore));
            }

            double promPrecision = sumaPrecision / n;
            double promRecall = sumaRecall / n;
            double promAccuracy = sumaAccuracy / n;
            double promFScore = sumaFScore / n;

            pwFinal.println("\nPROMEDIO GENERAL,,,,," + promPrecision + "," + promRecall + "," + promAccuracy + "," + promFScore);
            pwFinal.close();

            txtAreaResultados.append(String.format("\nPROMEDIO GENERAL -> Precision: %.2f, Recall: %.2f, Accuracy: %.2f, F-Score: %.2f\n",
                    promPrecision, promRecall, promAccuracy, promFScore));
            txtAreaResultados.append("Archivos CSV generados y guardados con éxito.\n");

            btnPasoAPaso.setEnabled(false);
            btnProcesarRestante.setEnabled(false);
            btnModoCompleto.setEnabled(false);
            btnTerminar.setEnabled(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al procesar métricas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reiniciarProceso() {
        indiceActual = 0;
        modoPasoActivo = false;
        txtAreaResultados.setText("Sistema reiniciado. Cargue un archivo CSV nuevo para comenzar.\n");
        btnPasoAPaso.setEnabled(false);
        btnProcesarRestante.setEnabled(false);
        btnTerminar.setEnabled(false);
        btnModoCompleto.setEnabled(false);
        modeloTablaMatriz.setRowCount(0);
        modeloTablaMatriz.setColumnCount(0);
    }

    private static class RegistroDato {
        String id;
        String real;
        String prediccion;

        public RegistroDato(String id, String real, String prediccion) {
            this.id = id;
            this.real = real;
            this.prediccion = prediccion;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EvaluadorClasificacionApp().setVisible(true);
        });
    }
}