// Prototype, will probably fold into Easy ML and relicense as MPL2 at some point

use std::any::TypeId;

// A named tensor http://nlp.seas.harvard.edu/NamedTensor
pub struct Tensor<T> {
    data: Vec<T>,
    dimensions: Vec<(Dimension, usize)>,
}

#[derive(Copy, Clone, Debug, Eq, PartialEq, Ord, PartialOrd)]
pub struct Dimension {
    name: TypeId,
}

impl Dimension {
    pub fn new<T: 'static>() -> Self {
        Dimension {
            name: TypeId::of::<T>(),
        }
    }
}

pub fn dimension<T: 'static>() -> Dimension {
    Dimension::new::<T>()
}

pub fn of_dimension<T: 'static>(length: usize) -> (Dimension, usize) {
    (dimension::<T>(), length)
}

fn has_duplicates(dimensions: &[(Dimension, usize)]) -> bool {
    for i in 1..dimensions.len() {
        let name = dimensions[i - 1].0;
        if dimensions[i..].iter().any(|d| d.0 == name) {
            return true;
        }
    }
    false
}

impl<T> Tensor<T> {
    #[track_caller]
    pub fn new(data: Vec<T>, dimensions: Vec<(Dimension, usize)>) -> Tensor<T> {
        assert_eq!(
            data.len(),
            dimensions.iter().map(|d| d.1).fold(1, |d1, d2| d1 * d2),
            "Length of dimensions must match size of data"
        );
        assert!(
            !has_duplicates(&dimensions),
            "Dimension names must all be unique"
        );

        Tensor { data, dimensions }
    }
}

#[test]
fn new_test() {
    struct X;
    struct Y;
    Tensor::new(
        vec![1, 2, 3, 4],
        vec![of_dimension::<X>(2), of_dimension::<Y>(2)],
    );
}

#[test]
#[should_panic]
fn repeated_name() {
    struct X;
    Tensor::new(
        vec![1, 2, 3, 4],
        vec![of_dimension::<X>(2), of_dimension::<X>(2)],
    );
}

#[test]
#[should_panic]
fn wrong_size() {
    struct X;
    Tensor::new(
        vec![1, 2, 3, 4],
        vec![of_dimension::<X>(2), of_dimension::<X>(3)],
    );
}
