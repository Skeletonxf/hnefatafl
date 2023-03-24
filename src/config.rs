use serde::{Serialize, Deserialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct Config {
    locale: String,
}

impl Config {
    pub fn from(toml: &str) -> Result<Self, toml::de::Error> {
        toml::from_str(toml)
    }
}
