# CV Criptografía - Ruben Montufar

Aplicación web Flask con CV personal y funcionalidad de descarga/subida de llaves públicas.

## Despliegue en Render

### Pasos para desplegar:

1. **Crear cuenta en Render**: https://render.com

2. **Conectar repositorio Git**:
   - Sube tu proyecto a GitHub (si aún no lo has hecho)
   - En Render, crea un nuevo "Web Service"
   - Conecta tu repositorio de GitHub

3. **Configuración en Render**:
   - **Environment**: Python 3
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `gunicorn app:app`
   - **Environment Variables**: 
     - Agrega `SECRET_KEY` con un valor secreto aleatorio

4. **Archivos necesarios** (ya creados):
   - `requirements.txt`: Dependencias de Python
   - `Procfile`: Comando para iniciar la aplicación
   - `.gitignore`: Archivos a ignorar en Git
   - `publicRuben.key`: Llave pública para descargar

### Comandos para subir a GitHub:

```bash
cd c:\Users\rmontufarr1800\Documents\practicas_c\criptografia
git init
git add .
git commit -m "Initial commit - Flask CV con criptografía"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/TU-REPOSITORIO.git
git push -u origin main
```

## Desarrollo local

```bash
python app.py
```

La aplicación estará disponible en http://localhost:5000
