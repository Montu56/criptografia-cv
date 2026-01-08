import base64
import random

# Generar números primos pequeños para ejemplo didáctico
def es_primo(n):
    if n < 2:
        return False
    for i in range(2, int(n**0.5) + 1):
        if n % i == 0:
            return False
    return True

def generar_primo(bits=8):
    while True:
        p = random.randint(2**(bits-1), 2**bits - 1)
        if es_primo(p):
            return p

# RSA simplificado para demostración
p = generar_primo(16)
q = generar_primo(16)
n = p * q
e = 65537  # Exponente público estándar

# Crear formato PEM-like
public_key = f"""-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwK8vXMqR7xK0J3pY5xQb
kzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2
xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9x
K0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH
+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL
8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5x
QbkzN8gFmH+TY3pQ9wK2xF5rN4mL8vP3qW2R9xK0J3pY5xQbkzN8gFmH+TY3pQ9w
K2xF5rN4mL8vP3qW2QIDAQABrmontufar1800
-----END PUBLIC KEY-----"""

print(public_key)
print(f"\nParámetros RSA (para el ejercicio):")
print(f"p = {p}")
print(f"q = {q}")
print(f"n = {n}")
print(f"e = {e}")
