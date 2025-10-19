use serde::{Deserialize, Serialize};
use std::convert::TryFrom;
use std::default::Default;

#[derive(Debug, Serialize, Deserialize)]
pub struct Config {
    pub locale: String,
}

impl TryFrom<&str> for Config {
    type Error = toml::de::Error;

    fn try_from(toml: &str) -> Result<Config, Self::Error> {
        toml::from_str(&toml)
    }
}

impl TryFrom<&Config> for String {
    type Error = toml::ser::Error;

    fn try_from(config: &Config) -> Result<String, Self::Error> {
        toml::to_string(config)
    }
}

impl Default for Config {
    fn default() -> Self {
        Config {
            locale: "en-GB".to_string(),
        }
    }
}
