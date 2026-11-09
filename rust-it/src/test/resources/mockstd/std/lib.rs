// A minimal `std` for the flow tests: the prelude of `core`.
pub use core::{clone, marker, ops, option, result};

pub mod prelude {
    pub mod v1 {
        pub use core::prelude::v1::*;
    }

    pub mod rust_2021 {
        pub use core::prelude::v1::*;
    }
}
