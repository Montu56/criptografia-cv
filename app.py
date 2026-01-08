from flask import Flask, render_template, send_file
import os

app = Flask(__name__)

# Forzar que Flask recargue las plantillas cuando cambian
app.config['TEMPLATES_AUTO_RELOAD'] = True

@app.route("/")
def index():
    return render_template("index.html")  # usa templates/index.html

@app.route("/descargar-llave")
def descargar_llave():
    # Crear el archivo public.key si no existe
    public_key_path = os.path.join(app.root_path, 'static', 'public.key')
    
    # Contenido de la llave pública
    public_key_content = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwK8vXMqR7xK0J3pY5xQb
kzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2
xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9x
K0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH
+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL
8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5x
QbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9w
K2xF5rN4mL8vP3qW2QIDAQABrmontufar1800
-----END PUBLIC KEY-----"""
    
    # Escribir el archivo
    with open(public_key_path, 'w') as f:
        f.write(public_key_content)
    
    return send_file(
        public_key_path,
        as_attachment=True,
        download_name='public.key',
        mimetype='application/x-pem-file'
    )

if __name__ == "__main__":
    app.run(debug=True)
