package io.github.skeletonxf.ui.state

interface StateHolder {
    interface StatefulLoading {
        val isLoading: Boolean
        val error: Throwable?

        val isError: Boolean
            get() = error != null
    }
}