package app.siatka

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

object Security {
    private const val ROUNDS = 12_000
    private val rng = SecureRandom()

    fun hashPassword(password: String): String {
        val salt = ByteArray(16).also { rng.nextBytes(it) }
        return encode("pbkdf2", salt, pbkdf2(password, salt))
    }

    fun verifyPassword(password: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        if (!stored.startsWith("pbkdf2$")) {
            return stored == password
        }
        val parts = stored.split("$")
        if (parts.size != 3) return false
        val salt = b64(parts[1])
        val expect = b64(parts[2])
        val got = pbkdf2(password, salt)
        return MessageDigest.isEqual(expect, got)
    }

    fun needsRehash(stored: String?): Boolean = stored != null && !stored.startsWith("pbkdf2$")

    fun randomPin(): String = "%06d".format(rng.nextInt(1_000_000))

    fun hmacHex(secret: String, body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(body.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun hmacOk(secret: String, body: String, hex: String): Boolean {
        val a = hmacHex(secret, body).lowercase()
        val b = hex.lowercase()
        if (a.length != b.length) return false
        return MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }

    fun derivePairKey(pin: String, address: String): String {
        val salt = MessageDigest.getInstance("SHA-256").digest("wataha|$address".toByteArray())
        return Base64.encodeToString(pbkdf2(pin, salt.copyOf(16)), Base64.NO_WRAP)
    }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ROUNDS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun encode(kind: String, salt: ByteArray, hash: ByteArray) =
        "$kind$${Base64.encodeToString(salt, Base64.NO_WRAP)}$${Base64.encodeToString(hash, Base64.NO_WRAP)}"

    private fun b64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    fun clipLog(s: String, n: Int = 180) = s.substring(0, min(s.length, n))
}
