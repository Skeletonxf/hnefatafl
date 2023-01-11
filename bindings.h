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
struct TileArray *game_state_handle_tiles(const struct GameStateHandle *handle);

/**
 * Returns a value from the array
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
