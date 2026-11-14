
/// `Self` in a struct impl: constructor, associated const, method chaining.

pub struct Point {
    x: i32,
    y: i32,
}

impl Point {
    pub const ORIGIN: Self = Self { x: 0, y: 0 };

    pub fn new(x: i32, y: i32) -> Self {
        Self { x, y }
    }

    pub fn moved(self, dx: i32, dy: i32) -> Self {
        Self::new(self.x + dx, self.y + dy)
    }

    pub fn is_origin(&self) -> bool {
        self.x == Self::ORIGIN.x
    }
}

/// `Self` in an enum: variants through `Self::`, matching on `Self::Variant`.

pub enum Shape {
    Circle { center: Point, radius: u32 },
    Segment(Point, Point),
    Empty,
}

impl Shape {
    pub fn circle(radius: u32) -> Self {
        Self::Circle { center: Point::ORIGIN, radius }
    }

    pub fn tag(&self) -> u32 {
        match self {
            Self::Circle { radius, .. } => *radius,
            Self::Segment(_, _) => 1,
            Self::Empty => 0,
        }
    }
}

/// `Self` in a trait: `Self: Sized` bound, `Self` return, default method using `Self::`.
pub trait Builder: Sized {
    type Item;

    fn empty() -> Self;
    fn push(self, item: Self::Item) -> Self;

    fn from_pair(first: Self::Item, second: Self::Item) -> Self {
        Self::push(Self::push(Self::empty(), first), second)
    }
}


pub struct Path {
    last: Option<Point>,
    len: u32,
}

impl Builder for Path {
    type Item = Point;

    fn empty() -> Self {
        Self { last: Option::None, len: 0 }
    }

    fn push(self, item: Self::Item) -> Self {
        Self { last: Option::Some(item), len: self.len + 1 }
    }
}

/// `Self` inside a generic impl and in a where clause.
pub struct Wrapper<T>(T);

impl<T: Clone> Wrapper<T>
where
    Self: Sized,
{
    pub fn duplicate(&self) -> (Self, Self) {
        (Self(self.0.clone()), Self(self.0.clone()))
    }
}

/// `Self` produced by a macro: the expansion must still resolve `Self` to `Counter`.
macro_rules! make_new {
    ($($field:ident),*) => {
        pub fn new() -> Self {
            Self { $($field: 0),* }
        }
    };
}

pub struct Counter {
    hits: u32,
    misses: u32,
}

impl Counter {
    make_new!(hits, misses);

    pub fn total(&self) -> u32 {
        self.hits + self.misses
    }
}
