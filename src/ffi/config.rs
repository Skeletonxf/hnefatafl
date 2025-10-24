use crate::config::Config;
use crate::uniffi;

use std::convert::{TryFrom, TryInto};
use std::fmt;
use std::sync::Mutex;

/// A handle to the config state behind a mutex to allow calling from Kotlin without issue
#[derive(Debug, uniffi::Object)]
pub struct ConfigHandle {
    config: Mutex<Config>,
}

#[uniffi::export]
impl ConfigHandle {
    /// Creates a config handle from a TOML file that was already saved
    #[uniffi::constructor]
    fn new(toml: &str) -> Result<ConfigHandle, DeserializeError> {
        match Config::try_from(toml) {
            Ok(config) => Ok(ConfigHandle {
                config: Mutex::new(config),
            }),
            Err(error) => Err(DeserializeError::Error(error.message().to_owned())),
        }
    }

    /// Creates a config handle from the defaults
    #[uniffi::constructor]
    fn default() -> ConfigHandle {
        ConfigHandle {
            config: Mutex::new(Config::default()),
        }
    }

    /// Gets the value of a key
    fn key(&self, for_key: ConfigStringKey) -> String {
        let config = self
            .config
            .lock()
            .expect("Poisoned mutex in ConfigHandle key");
        match for_key {
            ConfigStringKey::Locale => config.locale.clone(),
        }
    }

    /// Sets the value of a key
    fn set(&self, for_key: ConfigStringKey, value: String) {
        let mut config = self
            .config
            .lock()
            .expect("Poisoned mutex in ConfigHandle key");
        match for_key {
            ConfigStringKey::Locale => config.locale = value,
        }
    }

    /// Gets the value of the TOML file
    fn as_toml(&self) -> Result<String, SerializeError> {
        let guard = self
            .config
            .lock()
            .expect("Poisoned mutex in ConfigHandle key");
        let config: &Config = &guard;
        let converted: Result<String, toml::ser::Error> = config.try_into();
        converted.map_err(|error| SerializeError::Error(error.to_string()))
    }
}

#[derive(Clone, Debug, Eq, PartialEq, uniffi::Enum)]
enum DeserializeError {
    Error(String),
}

impl fmt::Display for DeserializeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            DeserializeError::Error(error) => {
                write!(f, "Error trying to deserialize TOML: {}", error)
            }
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq, uniffi::Enum)]
enum SerializeError {
    Error(String),
}

impl fmt::Display for SerializeError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            SerializeError::Error(error) => {
                write!(f, "Error trying to serialize TOML: {}", error)
            }
        }
    }
}

#[repr(u8)]
#[derive(Clone, Copy, Debug, uniffi::Enum)]
pub enum ConfigStringKey {
    Locale = 0,
}
