#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

enum Tile {
  Empty = 0,
  Attacker = 1,
  Defender = 2,
  King = 3,
};
typedef uint8_t Tile;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____TileArray FFIResult_____TileArray;

typedef struct GameStateHandle GameStateHandle;

/**
 * An array of tiles.
 */
typedef struct TileArray TileArray;

/**
 * Creates a new GameStateHandle
 */
struct GameStateHandle *game_state_handle_new(void);

/**
 * Destroys the data owned by the pointer
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void game_state_handle_destroy(struct GameStateHandle *handle);

/**
 * Prints the game state
 */
void game_state_handle_debug(const struct GameStateHandle *handle);

/**
 * Returns the tiles in row major order
 */
struct FFIResult_____TileArray *game_state_handle_tiles(const struct GameStateHandle *handle);

/**
 * Returns the length of one side of the grid
 */
uintptr_t game_state_handle_grid_size(const struct GameStateHandle *handle);

/**
 * Returns a value from the array, or Empty if out of bounds
 */
Tile tile_array_get(const struct TileArray *array, uintptr_t index);

/**
 * Returns the length of the array
 */
uintptr_t tile_array_length(const struct TileArray *array);

/**
 * Destroys the data owned by the TileArray
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void tile_array_destroy(struct TileArray *array);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
bool result_tile_array_is_ok(struct FFIResult_____TileArray *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
struct TileArray *result_tile_array_get_ok(struct FFIResult_____TileArray *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_tile_array_get_error(struct FFIResult_____TileArray *result);
