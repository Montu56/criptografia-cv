from flask import Flask, render_template, send_file, request, redirect, url_for, flash
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Forzar que Flask recargue las plantillas cuando cambian
app.config['TEMPLATES_AUTO_RELOAD'] = True
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'tu_clave_secreta_aqui')
app.config['UPLOAD_FOLDER'] = 'uploads'
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max

# Crear carpeta de uploads si no existe
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

ALLOWED_EXTENSIONS = {'key', 'pem', 'pub', 'txt'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/subir-archivo", methods=['POST'])
def subir_archivo():
    # Verificar si se envió un archivo
    if 'archivo' not in request.files:
        flash('No se seleccionó ningún archivo', 'error')
        return redirect(url_for('index'))
    
    file = request.files['archivo']
    
    # Verificar si el archivo tiene nombre
    if file.filename == '':
        flash('No se seleccionó ningún archivo', 'error')
        return redirect(url_for('index'))
    
    # Verificar si el archivo es permitido
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        flash(f'Archivo {filename} subido exitosamente!', 'success')
        return redirect(url_for('index'))
    else:
        flash('Tipo de archivo no permitido. Solo se permiten: .key, .pem, .pub, .txt', 'error')
        return redirect(url_for('index'))

@app.route("/descargar-llave")
def descargar_llave():
    # Ruta al archivo de llave pública de Ruben (relativa al proyecto)
    public_key_path = os.path.join(app.root_path, 'publicRuben.key')
    
    # Verificar si existe el archivo
    if os.path.exists(public_key_path):
        return send_file(
            public_key_path,
            as_attachment=True,
            download_name='publicRuben.key',
            mimetype='application/x-pem-file'
        )
    else:
        flash('La llave pública no está disponible', 'error')
        return redirect(url_for('index'))

if __name__ == "__main__":
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=False)
