use crate::uniffi;
use license_fetcher::read_package_list_from_out_dir;
use serde_json;
use std::error::Error;
use std::fmt;

#[derive(Clone, Debug, uniffi::Enum)]
pub enum LicensesError {
    PackageList(String),
    Serialization(String),
}

impl fmt::Display for LicensesError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            LicensesError::PackageList(error) => {
                write!(f, "Error getting licenses: {}", error)
            }
            LicensesError::Serialization(error) => {
                write!(f, "Error serializing licenses: {}", error)
            }
        }
    }
}

impl Error for LicensesError {}

#[uniffi::export]
fn licenses_json() -> Result<String, LicensesError> {
    read_package_list_from_out_dir!()
        .map_err(|error| LicensesError::PackageList(error.to_string()))
        .and_then(|package_list| {
            serde_json::to_string(&package_list)
                .map_err(|error| LicensesError::Serialization(error.to_string()))
        })
}
