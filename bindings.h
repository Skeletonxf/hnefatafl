typedef struct GameStateHandle GameStateHandle;

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

void hello_from_rust(void);
