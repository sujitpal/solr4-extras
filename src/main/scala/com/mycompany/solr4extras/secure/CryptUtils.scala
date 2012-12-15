package com.mycompany.solr4extras.secure

import java.security.SecureRandom

import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}
import javax.crypto.Cipher

/**
 * Methods for generating random symmetric encryption keys,
 * and encrypting and decrypting text using these keys.
 */
object CryptUtils {

  def keys(): (Array[Byte], Array[Byte]) = {
    val rand = new SecureRandom
    val key = new Array[Byte](16)
    val iv = new Array[Byte](16)
    rand.nextBytes(key)
    rand.nextBytes(iv)
    (key, iv)
  }

  def encrypt(data: Array[Byte], 
      key: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    val keyspec = new SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    if (iv == null) 
      cipher.init(Cipher.ENCRYPT_MODE, keyspec)
    else
      cipher.init(Cipher.ENCRYPT_MODE, keyspec, 
        new IvParameterSpec(iv))
    cipher.doFinal(data)
  }
  
  def decrypt(encdata: Array[Byte], key: Array[Byte],
      initvector: Array[Byte]): String = {
    val keyspec = new SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, keyspec, 
      new IvParameterSpec(initvector))
    val decrypted = cipher.doFinal(encdata)
    new String(decrypted, 0, decrypted.length)
  }
}