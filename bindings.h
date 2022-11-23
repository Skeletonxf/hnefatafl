typedef struct GameStateHandle GameStateHandle;

struct GameStateHandle *game_state_handle_new(void);

void game_state_handle_destroy(struct GameStateHandle *handle);

void game_state_handle_debug(struct GameStateHandle *handle);

void hello_from_rust(void);
