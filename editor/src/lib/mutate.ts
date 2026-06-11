/** A scoped mutator: run `fn` against a draft of some part of the project, committed by the store. */
export type Mut<T> = (fn: (draft: T) => void) => void
