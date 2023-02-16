import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.ffi.FFIError
import java.lang.foreign.MemoryAddress

private const val FFI_RESULT_TYPE_OK: Byte = 0
private const val FFI_RESULT_TYPE_ERROR: Byte = 1
private const val FFI_RESULT_TYPE_NULL: Byte = 2

fun <T, E> KResult.Companion.from(
    handle: MemoryAddress,
    get_type: (MemoryAddress) -> Byte,
    get_ok: (MemoryAddress) -> T,
    get_err: (MemoryAddress) -> E,
): KResult<T, FFIError<E?>> = when (get_type(handle)) {
    FFI_RESULT_TYPE_OK -> KResult.Ok(get_ok(handle))
    FFI_RESULT_TYPE_ERROR -> KResult.Error(FFIError("Data was error", get_err(handle)))
    FFI_RESULT_TYPE_NULL -> KResult.Error(FFIError("Data was null or corrupted", null))
    else -> KResult.Error(FFIError("Unrecognised FFI result type", null))
}