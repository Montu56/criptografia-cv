from Crypto.PublicKey import RSA

# Generar par de llaves RSA de 2048 bits
key = RSA.generate(2048)

# Extraer llave privada y pública
private_key = key.export_key()
public_key = key.publickey().export_key()

# Guardar llave privada
with open('private.pem', 'wb') as f:
    f.write(private_key)

# Guardar llave pública
with open('public.pem', 'wb') as f:
    f.write(public_key)

print("✓ Llaves RSA de 2048 bits generadas exitosamente!")
print("\nLlave pública:")
print(public_key.decode())
print("\n¡IMPORTANTE! Guarda 'private.pem' en un lugar seguro y NO la compartas.")
