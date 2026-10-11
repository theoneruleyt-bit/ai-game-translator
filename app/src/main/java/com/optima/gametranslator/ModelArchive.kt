package com.optima.gametranslator

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Fixed archive root, bounded expansion, no directory traversal, publish only after validation. */
object ModelArchive {
    const val ROOT="vosk-model-small-en-us-0.15"
    fun destination(directory: File,name: String,root: String=ROOT): File {
        require(name=="$root/"||name.startsWith("$root/")) { "Unexpected archive root" }
        val base=directory.canonicalFile
        val output=File(base,name).canonicalFile
        require(output.path.startsWith(base.path+File.separator)) { "Archive path escapes directory" }
        require(output.path==File(base,root).path||output.path.startsWith(File(base,root).path+File.separator))
        return output
    }
    fun unpack(input: InputStream,directory: File,rootName: String=ROOT,expandedLimit: Long=256L*1024*1024,canceled: ()->Boolean) {
        var bytes=0L;var entries=0
        ZipInputStream(input).use { zip ->
            val buffer=ByteArray(32768)
            while(true) {
                check(!canceled()) { "Canceled" }
                val entry=zip.nextEntry?:break
                require(++entries<=5000)
                val output=destination(directory,entry.name,rootName)
                if(entry.isDirectory) check(output.mkdirs()||output.isDirectory)
                else {
                    check(output.parentFile!!.mkdirs()||output.parentFile!!.isDirectory)
                    output.outputStream().use { target ->
                        while(true) {
                            check(!canceled()) { "Canceled" }
                            val count=zip.read(buffer);if(count<0)break
                            bytes+=count;require(bytes<=expandedLimit)
                            target.write(buffer,0,count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val root=File(directory,rootName)
        fun present(name: String)=File(root,name).let { it.isFile&&it.length()>0 }
        require(present("am/final.mdl")&&present("conf/model.conf")&&
            (present("graph/HCLG.fst") || present("graph/HCLr.fst")&&present("graph/Gr.fst"))) { "Incomplete model" }
    }
}
