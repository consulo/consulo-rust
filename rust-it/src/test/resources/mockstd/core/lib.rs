// A minimal `core` for the flow tests: the lang items the borrow checker asks for, and the prelude.
#![no_core]

pub mod marker {
    #[lang = "sized"]
    pub trait Sized {}

    #[lang = "copy"]
    pub trait Copy: crate::clone::Clone {}
}

pub mod clone {
    #[lang = "clone"]
    pub trait Clone: crate::marker::Sized {
        fn clone(&self) -> Self;
    }
}

pub mod ops {
    #[lang = "drop"]
    pub trait Drop {
        fn drop(&mut self);
    }
}

pub mod option {
    pub enum Option<T> {
        None,
        Some(T),
    }
}

pub mod result {
    pub enum Result<T, E> {
        Ok(T),
        Err(E),
    }
}

pub mod prelude {
    pub mod v1 {
        pub use crate::clone::Clone;
        pub use crate::marker::{Copy, Sized};
        pub use crate::ops::Drop;
        pub use crate::option::Option::{self, None, Some};
        pub use crate::result::Result::{self, Err, Ok};
    }

    pub mod rust_2021 {
        pub use super::v1::*;
    }
}
