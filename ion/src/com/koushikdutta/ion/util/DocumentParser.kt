package com.koushikdutta.ion.util

import com.koushikdutta.scratch.AsyncRead
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.buffers.ByteBufferListInputStream
import com.koushikdutta.scratch.http.AsyncHttpMessageBody
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.text.Charsets.UTF_8


/**
 * Created by koush on 8/3/13.
 */
class DocumentParser : AsyncParser<Document> {
    override fun parse(read: AsyncRead): Promise<Document> = ByteBufferListParser().parse(read).then {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteBufferListInputStream(it))
    }

    override val contentType = "application/xml"
    override val type = Document::class.java
}

class DocumentSerializer : AsyncSerializer<Document> {
    override fun write(value: Document): Promise<AsyncHttpMessageBody> {
        val source = DOMSource(value)
        val tf: TransformerFactory = TransformerFactory.newInstance()
        val transformer: Transformer = tf.newTransformer()
        val bout = ByteArrayOutputStream()
        val writer = OutputStreamWriter(bout, UTF_8)
        val result = StreamResult(writer)
        transformer.transform(source, result)
        writer.flush()

        return ByteBufferListSerializer("application/xml").write(ByteBufferList(ByteBuffer.wrap(bout.toByteArray())))
    }
}
