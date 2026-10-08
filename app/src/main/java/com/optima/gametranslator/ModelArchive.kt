package com.optima.gametranslator

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Fixed archive root, bounded expansion, no directory traversal, publish only after validation. */
object ModelArchive {
    const val ROOT="vosk-model-small-en-us-0.15"
    fun destination(directory: File,name: String): File {
        require(name=="$ROOT/"||name.startsWith("$ROOT/")) { "Unexpected archive root" }
        val base=directory.canonicalFile
        val output=File(base,name).canonicalFile
        require(output.path.startsWith(base.path+File.separator)) { "Archive path escapes directory" }
        require(output.path==File(base,ROOT).path||output.path.startsWith(File(base,ROOT).path+File.separator))
        return output
    }
    fun unpack(input: InputStream,directory: File,canceled: ()->Boolean) {
        var bytes=0L;var entries=0
        ZipInputStream(input).use { zip ->
            val buffer=ByteArray(32768)
            while(true) {
                check(!canceled()) { "Canceled" }
                val entry=zip.nextEntry?:break
                require(++entries<=5000)
                val output=destination(directory,entry.name)
                if(entry.isDirectory) check(output.mkdirs()||output.isDirectory)
                else {
                    check(output.parentFile!!.mkdirs()||output.parentFile!!.isDirectory)
                    output.outputStream().use { target ->
                        while(true) {
                            check(!canceled()) { "Canceled" }
                            val count=zip.read(buffer);if(count<0)break
                            bytes+=count;require(bytes<=256L*1024*1024)
                            target.write(buffer,0,count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val root=File(directory,ROOT)
        require(listOf("am/final.mdl","conf/model.conf","graph/HCLr.fst","graph/Gr.fst")
            .all { File(root,it).isFile&&File(root,it).length()>0 }) { "Incomplete model" }
    }
}
