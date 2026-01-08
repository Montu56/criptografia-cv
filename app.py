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
    # Leer la llave pública real del archivo
    public_key_path = os.path.join(app.root_path, 'public.pem')
    
    if not os.path.exists(public_key_path):
        # Si no existe, usar contenido por defecto
        public_key_content = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj14Fo19asmPiSCYiTRI6
gaq4n9fD1IZdXx7xbrfJK4YF0yZU+Jx7+5cYOW6fCKWMlsIz1pNn2ZiexEyT7SS2
mjKZkSkGVtxQh/zIDnki38JRVd+spFqgYdNCerU5Z86RTvdxxZhXNMblziy3FEli
Y7ok6HdSKhR2NsyrtvY/q1HjY0QOvyIvgIBQAgciaPNQhs4e0jQhRCGThrYTd9W3
R6v/M8bI8Fb+zj9UMxx4hbtTVfhwgkdIq9yklcywei6i/Ax6EcIfMUrTKJst9clJ
wM1YC8mzM9HtrMtUX1Eizadqqz9vgjA7vY+GXfjxnmGytH4Q+w9SA4Y6EPT4giYr
5QIDAQAB
-----END PUBLIC KEY-----"""
        with open(public_key_path, 'w') as f:
            f.write(public_key_content)
    else:
        with open(public_key_path, 'r') as f:
            public_key_content = f.read()
    
    # Crear archivo temporal para descarga
    download_path = os.path.join(app.root_path, 'static', 'public.key')
    with open(download_path, 'w') as f:
        f.write(public_key_content)
    
    return send_file(
        download_path,
        as_attachment=True,
        download_name='rmontufar_public.key',
        mimetype='application/x-pem-file'
    )

if __name__ == "__main__":
    app.run(debug=True)
