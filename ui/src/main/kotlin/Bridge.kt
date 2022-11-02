import jdk.incubator.foreign.CLinker

class Bridge {
    fun getLinkerInstance() {
        val linker = CLinker.getInstance()
    }
}