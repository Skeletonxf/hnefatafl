#!/bin/sh
CARGO_PROFILE_RELEASE_DEBUG=true cargo flamegraph --unit-test hnefatafl -- bot::minmax::defenders_minmax_takes_the_winning_move
