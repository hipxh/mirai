package net.mamoe.mirai.message

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.QQ
import net.mamoe.mirai.message.defaults.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.*


/**
 * 可发送的或从服务器接收的消息.
 * 采用这样的消息模式是因为 QQ 的消息多元化, 一条消息中可包含 [纯文本][PlainText], [图片][Image] 等.
 *
 * #### 在 Kotlin 使用 [Message]
 *  这与使用 [String] 的使用非常类似.
 *
 *  比较 [Message] 与 [String] (使用 infix [Message.eq]):
 *  `if(message eq "你好") qq.sendMessage(message)`
 *
 *  连接 [Message] 与 [Message], [String], [BufferedImage] (使用 operator [Message.plus]):
 *  ```
 *      message = PlainText("Hello ")
 *      qq.sendMessage(message + "world")
 *  ```
 *
 * @author Him188moe
 * @see Contact.sendMessage
 */
abstract class Message {
    internal abstract val type: MessageKey

    private var toStringCache: String? = null
    private val cacheLock = object : Any() {}

    internal abstract fun toStringImpl(): String

    /**
     * 得到用户层的文本消息. 如:
     * - [PlainText] 得到 消息内容
     * - [Image] 得到 "{ID}.png"
     * - [At] 得到 "[@qq]"
     */
    final override fun toString(): String {
        synchronized(cacheLock) {
            if (toStringCache != null) {
                return toStringCache!!
            }

            this.toStringCache = toStringImpl()
            return toStringCache!!
        }
    }

    internal fun clearToStringCache() {
        synchronized(cacheLock) {
            toStringCache = null
        }
    }

    /**
     * 得到类似 "PlainText(内容)", "Image(ID)"
     */
    open fun toObjectString(): String {
        return this.javaClass.simpleName + String.format("(%s)", this.toString())
    }

    /**
     * 转换为数据包使用的 byte array
     */
    abstract fun toByteArray(): ByteArray


    /**
     * 比较两个 Message 的内容是否相等. 如:
     * - [PlainText] 比较 [PlainText.text]
     * - [Image] 比较 [Image.imageId]
     */
    abstract infix fun eq(another: Message): Boolean

    /**
     * 将这个消息的 [toString] 与 [another] 比较
     */
    infix fun eq(another: String): Boolean = this.toString() == another

    /**
     * 判断 [sub] 是否存在于本消息中
     */
    abstract operator fun contains(sub: String): Boolean

    /**
     * 把这个消息连接到另一个消息的头部. 相当于字符串相加
     *
     *
     * Connects this Message to the head of another Message.
     * That is, another message becomes the tail of this message.
     * This method does similar to [String.concat]
     *
     *
     * E.g.:
     * PlainText a = new PlainText("Hello ");
     * PlainText b = new PlainText("world");
     * PlainText c = a.concat(b);
     *
     *
     * the text of c is "Hello world"
     *
     * @param tail tail
     * @return message connected
     */
    open fun concat(tail: Message): MessageChain {
        if (tail is MessageChain) {
            return MessageChain(this).let {
                tail.list.forEach { child -> it.concat(child) }
                it
            }
        }
        return MessageChain(this, Objects.requireNonNull(tail))
    }

    fun concat(tail: String): MessageChain {
        return concat(PlainText(tail))
    }


    infix fun withImage(imageId: String): MessageChain = this + Image(imageId)
    fun withImage(filename: String, image: BufferedImage): MessageChain = this + UnsolvedImage(filename, image)
    infix fun withImage(imageFile: File): MessageChain = this + UnsolvedImage(imageFile)

    infix fun withAt(target: QQ): MessageChain = this + target.at()
    infix fun withAt(target: Long): MessageChain = this + At(target)


    open fun toChain(): MessageChain {
        return MessageChain(this)
    }


    /* For Kotlin */

    /**
     * 实现使用 '+' 操作符连接 [Message] 与 [Message]
     */
    infix operator fun plus(another: Message): MessageChain = this.concat(another)

    /**
     * 实现使用 '+' 操作符连接 [Message] 与 [String]
     */
    infix operator fun plus(another: String): MessageChain = this.concat(another)

    /**
     * 实现使用 '+' 操作符连接 [Message] 与 [Number]
     */
    infix operator fun plus(another: Number): MessageChain = this.concat(another.toString())

    /**
     * 连接 [String] 与 [Message]
     */
    fun String.concat(another: Message): MessageChain = PlainText(this).concat(another)

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false

        if (type != other.type) return false

        return this.toString() == other.toString()
    }
}