#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

enum ConfigStringKey {
  Locale = 0,
};
typedef uint8_t ConfigStringKey;

enum FFIResultType {
  Ok = 0,
  Err = 1,
  Null = 2,
};
typedef uint8_t FFIResultType;

enum GameStateUpdate {
  DefenderWin = 0,
  AttackerWin = 1,
  DefenderCapture = 2,
  AttackerCapture = 3,
  Nothing = 4,
};
typedef uint8_t GameStateUpdate;

enum Tile {
  Empty = 0,
  Attacker = 1,
  Defender = 2,
  King = 3,
};
typedef uint8_t Tile;

enum TurnPlayer {
  DefendersTurn = 0,
  AttackersTurn = 1,
};
typedef uint8_t TurnPlayer;

enum Winner {
  Defenders = 0,
  Attackers = 1,
  None = 2,
};
typedef uint8_t Winner;

/**
 * An array of something
 */
typedef struct Array_Play Array_Play;

/**
 * An array of something
 */
typedef struct Array_Tile Array_Tile;

/**
 * An array of something
 */
typedef struct Array_u16 Array_u16;

typedef struct ConfigHandle ConfigHandle;

typedef struct FFIError FFIError;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_GameStateUpdate FFIResult_GameStateUpdate;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_TurnPlayer FFIResult_TurnPlayer;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_Winner FFIResult_Winner;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____ConfigHandle______FFIError FFIResult_____ConfigHandle______FFIError;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____FFIError FFIResult_____FFIError;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____PlayArray FFIResult_____PlayArray;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____TileArray FFIResult_____TileArray;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____UTF16Array FFIResult_____UTF16Array;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_____UTF16Array______FFIError FFIResult_____UTF16Array______FFIError;

/**
 * A wrapper around a result
 */
typedef struct FFIResult______ConfigHandle______FFIError FFIResult______ConfigHandle______FFIError;

/**
 * A wrapper around a result
 */
typedef struct FFIResult_u32 FFIResult_u32;

typedef struct GameStateHandle GameStateHandle;

typedef struct Vec_u16 Vec_u16;

/**
 * An array of tiles.
 */
typedef struct Array_Tile TileArray;

/**
 * A flattened representation of a Play, consisting of 4 u8s for a total size of 4 bytes
 */
typedef struct FlatPlay {
  uint8_t from_x;
  uint8_t from_y;
  uint8_t to_x;
  uint8_t to_y;
} FlatPlay;

/**
 * An array of plays.
 */
typedef struct Array_Play PlayArray;

/**
 * An array of UTF-16 characters.
 */
typedef struct Array_u16 UTF16Array;

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
uint8_t game_state_handle_grid_size(const struct GameStateHandle *handle);

/**
 * Returns the available plays
 */
struct FFIResult_____PlayArray *game_state_available_plays(const struct GameStateHandle *handle);

/**
 * Makes a play, if legal
 */
struct FFIResult_GameStateUpdate *game_state_handle_make_play(const struct GameStateHandle *handle,
                                                              uint8_t from_x,
                                                              uint8_t from_y,
                                                              uint8_t to_x,
                                                              uint8_t to_y);

/**
 * Returns the winner, if any
 */
struct FFIResult_Winner *game_state_handle_winner(const struct GameStateHandle *handle);

/**
 * Returns the player that is making the current turn
 */
struct FFIResult_TurnPlayer *game_state_current_player(const struct GameStateHandle *handle);

/**
 * Returns the turn count. Starts at 0 with Defenders going first, odd turn counts are Attackers'
 * turns.
 */
struct FFIResult_u32 *game_state_handle_turn_count(const struct GameStateHandle *handle);

/**
 * Returns the dead pieces in row major order (no Empty tiles will actually be in the array, but
 * the TileArray will still return Empty if indexed out of bounds)
 *
 * Running out of aliases for the enum variants, so adding a PieceArray type would be problematic
 */
struct FFIResult_____TileArray *game_state_handle_dead(const struct GameStateHandle *handle);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_game_state_update_get_type(struct FFIResult_GameStateUpdate *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
GameStateUpdate result_game_state_update_get_ok(struct FFIResult_GameStateUpdate *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_game_state_update_get_error(struct FFIResult_GameStateUpdate *result);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_u32_get_type(struct FFIResult_u32 *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
uint32_t result_u32_get_ok(struct FFIResult_u32 *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_u32_get_error(struct FFIResult_u32 *result);

/**
 * Creates a new ConfigHandle from a UTF-16 toml file
 */
struct FFIResult_____ConfigHandle______FFIError *config_handle_new(const uint16_t *chars,
                                                                   uintptr_t length);

/**
 * Creates a default ConfigHandle
 */
struct ConfigHandle *config_handle_default(void);

/**
 * Destroys the data owned by the pointer
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void config_handle_destroy(struct ConfigHandle *handle);

/**
 * Returns a vec of UTF-16 chars of the string key value
 */
struct FFIResult_____UTF16Array______FFIError *config_handle_get_string_key(const struct ConfigHandle *handle,
                                                                            ConfigStringKey key);

/**
 * Sets a UTF-16 string to the string key value
 */
struct FFIResult_____FFIError *config_handle_set_string_key(const struct ConfigHandle *handle,
                                                            ConfigStringKey key,
                                                            const uint16_t *chars,
                                                            uintptr_t length);

/**
 * Returns a vec of UTF-16 chars of the entire TOML data
 */
struct FFIResult_____UTF16Array______FFIError *config_handle_get_file(const struct ConfigHandle *handle);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_config_handle_get_type(struct FFIResult______ConfigHandle______FFIError *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
const struct ConfigHandle *result_config_handle_get_ok(struct FFIResult______ConfigHandle______FFIError *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
struct FFIError *result_config_handle_get_error(struct FFIResult______ConfigHandle______FFIError *result);

/**
 * Returns a vec of UTF-16 chars of the error info, reclaiming the memory for the FFIError.
 * Safety: calling this on an invalid or aliased pointer is undefined behavior
 */
struct Array_u16 *error_consume_info(struct FFIError *error);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_void_get_type(struct FFIResult_____FFIError *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
void result_void_get_ok(struct FFIResult_____FFIError *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
struct FFIError *result_void_get_error(struct FFIResult_____FFIError *result);

/**
 * Returns a value from the array, or Empty if out of bounds
 */
Tile tile_array_get(const TileArray *array, uintptr_t index);

/**
 * Returns the length of the array
 */
uintptr_t tile_array_length(const TileArray *array);

/**
 * Destroys the data owned by the TileArray
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void tile_array_destroy(TileArray *array);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_tile_array_get_type(struct FFIResult_____TileArray *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
TileArray *result_tile_array_get_ok(struct FFIResult_____TileArray *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_tile_array_get_error(struct FFIResult_____TileArray *result);

/**
 * Returns a value from the array, or a dummy all 0s Play if out of bounds
 */
struct FlatPlay play_array_get(const PlayArray *array, uintptr_t index);

/**
 * Returns the length of the array
 */
uintptr_t play_array_length(const PlayArray *array);

/**
 * Destroys the data owned by the PlayArray
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void play_array_destroy(PlayArray *array);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_play_array_get_type(struct FFIResult_____PlayArray *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
PlayArray *result_play_array_get_ok(struct FFIResult_____PlayArray *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_play_array_get_error(struct FFIResult_____PlayArray *result);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_winner_get_type(struct FFIResult_Winner *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
Winner result_winner_get_ok(struct FFIResult_Winner *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_winner_get_error(struct FFIResult_Winner *result);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_player_get_type(struct FFIResult_TurnPlayer *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
TurnPlayer result_player_get_ok(struct FFIResult_TurnPlayer *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_player_get_error(struct FFIResult_TurnPlayer *result);

/**
 * Destroys the data owned by the pointer
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void utf16_destroy(struct Vec_u16 *handle);

/**
 * Returns the length of the array
 */
uintptr_t utf16_array_length(const UTF16Array *array);

/**
 * Copies over all values from the array into the buffer
 * Safety: The caller is responsible for ensuring the buffer is at least as many bytes long as
 * the array and not aliased anywhere.
 */
void utf16_array_copy_to(const UTF16Array *array, uint16_t *buffer);

/**
 * Destroys the data owned by the UTF16Array
 * The caller is responsible for ensuring there are no aliased references elsewhere in the
 * program
 */
void utf16_array_destroy(UTF16Array *array);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_utf16_array_get_type(struct FFIResult_____UTF16Array *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
UTF16Array *result_utf16_array_get_ok(struct FFIResult_____UTF16Array *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
void result_utf16_array_get_error(struct FFIResult_____UTF16Array *result);

/**
 * Safety: calling this on an invalid pointer is undefined behavior
 */
FFIResultType result_utf16_array_error_get_type(struct FFIResult_____UTF16Array______FFIError *result);

/**
 * Safety: calling this on an invalid pointer or an Err variant is undefined behavior
 */
UTF16Array *result_utf16_array_error_get_ok(struct FFIResult_____UTF16Array______FFIError *result);

/**
 * Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
 */
struct FFIError *result_utf16_array_error_get_error(struct FFIResult_____UTF16Array______FFIError *result);
