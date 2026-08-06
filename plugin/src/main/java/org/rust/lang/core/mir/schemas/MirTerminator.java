/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public interface MirTerminator<BB extends MirBasicBlock> {
    @Nonnull
    MirSourceInfo getSource();

    default boolean isDummy() {
        return this == DUMMY;
    }

    @Nonnull
    List<MirBasicBlock> getSuccessors();

    /**
     * This is singleton because it is identified using reference identity (==)
     */
    MirTerminator<?> DUMMY = new Resume<>(MirSourceInfo.fake);

    final class Return<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirSourceInfo source;

        public Return(@Nonnull MirSourceInfo source) {
            this.source = source;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            return Collections.emptyList();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Return<?> that = (Return<?>) o;
            return Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source);
        }

        @Override
        public String toString() {
            return "Return(source=" + source + ")";
        }
    }

    final class Resume<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirSourceInfo source;

        public Resume(@Nonnull MirSourceInfo source) {
            this.source = source;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            return Collections.emptyList();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Resume<?> resume = (Resume<?>) o;
            return Objects.equals(source, resume.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source);
        }

        @Override
        public String toString() {
            return "Resume(source=" + source + ")";
        }
    }

    final class Assert<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirOperand cond;
        private final boolean expected;
        @Nonnull
        private final BB target;
        @Nonnull
        private final MirAssertKind msg;
        @Nullable
        private final BB unwind;
        @Nonnull
        private final MirSourceInfo source;

        public Assert(
            @Nonnull MirOperand cond,
            boolean expected,
            @Nonnull BB target,
            @Nonnull MirAssertKind msg,
            @Nullable BB unwind,
            @Nonnull MirSourceInfo source
        ) {
            this.cond = cond;
            this.expected = expected;
            this.target = target;
            this.msg = msg;
            this.unwind = unwind;
            this.source = source;
        }

        @Nonnull
        public MirOperand getCond() {
            return cond;
        }

        public boolean isExpected() {
            return expected;
        }

        @Nonnull
        public BB getTarget() {
            return target;
        }

        @Nonnull
        public MirAssertKind getMsg() {
            return msg;
        }

        @Nullable
        public BB getUnwind() {
            return unwind;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            List<MirBasicBlock> result = new ArrayList<>();
            result.add(target);
            if (unwind != null) result.add(unwind);
            return result;
        }

        @Nonnull
        public Assert<BB> copy(
            @Nullable MirOperand cond,
            @Nullable Boolean expected,
            @Nullable BB target,
            @Nullable MirAssertKind msg,
            @Nullable BB unwind,
            @Nullable MirSourceInfo source
        ) {
            return new Assert<>(
                cond != null ? cond : this.cond,
                expected != null ? expected : this.expected,
                target != null ? target : this.target,
                msg != null ? msg : this.msg,
                unwind != null ? unwind : this.unwind,
                source != null ? source : this.source
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Assert<?> anAssert = (Assert<?>) o;
            return expected == anAssert.expected
                && Objects.equals(cond, anAssert.cond)
                && Objects.equals(target, anAssert.target)
                && Objects.equals(msg, anAssert.msg)
                && Objects.equals(unwind, anAssert.unwind)
                && Objects.equals(source, anAssert.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cond, expected, target, msg, unwind, source);
        }

        @Override
        public String toString() {
            return "Assert(cond=" + cond + ", expected=" + expected + ", target=" + target + ", msg=" + msg + ", unwind=" + unwind + ")";
        }
    }

    final class Goto<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final BB target;
        @Nonnull
        private final MirSourceInfo source;

        public Goto(@Nonnull BB target, @Nonnull MirSourceInfo source) {
            this.target = target;
            this.source = source;
        }

        @Nonnull
        public BB getTarget() {
            return target;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            return Collections.singletonList(target);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Goto<?> goTo = (Goto<?>) o;
            return Objects.equals(target, goTo.target) && Objects.equals(source, goTo.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, source);
        }

        @Override
        public String toString() {
            return "Goto(target=" + target + ")";
        }
    }

    final class SwitchInt<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirOperand discriminant;
        @Nonnull
        private final MirSwitchTargets<BB> targets;
        @Nonnull
        private final MirSourceInfo source;

        public SwitchInt(
            @Nonnull MirOperand discriminant,
            @Nonnull MirSwitchTargets<BB> targets,
            @Nonnull MirSourceInfo source
        ) {
            this.discriminant = discriminant;
            this.targets = targets;
            this.source = source;
        }

        @Nonnull
        public MirOperand getDiscriminant() {
            return discriminant;
        }

        @Nonnull
        public MirSwitchTargets<BB> getTargets() {
            return targets;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            return Collections.unmodifiableList(targets.getTargets());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SwitchInt<?> that = (SwitchInt<?>) o;
            return Objects.equals(discriminant, that.discriminant)
                && Objects.equals(targets, that.targets)
                && Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(discriminant, targets, source);
        }

        @Override
        public String toString() {
            return "SwitchInt(discriminant=" + discriminant + ", targets=" + targets + ")";
        }
    }

    final class FalseEdge<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final BB realTarget;
        @Nullable
        private final BB imaginaryTarget;
        @Nonnull
        private final MirSourceInfo source;

        public FalseEdge(@Nonnull BB realTarget, @Nullable BB imaginaryTarget, @Nonnull MirSourceInfo source) {
            this.realTarget = realTarget;
            this.imaginaryTarget = imaginaryTarget;
            this.source = source;
        }

        @Nonnull
        public BB getRealTarget() {
            return realTarget;
        }

        @Nullable
        public BB getImaginaryTarget() {
            return imaginaryTarget;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            List<MirBasicBlock> result = new ArrayList<>();
            result.add(realTarget);
            if (imaginaryTarget != null) result.add(imaginaryTarget);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FalseEdge<?> that = (FalseEdge<?>) o;
            return Objects.equals(realTarget, that.realTarget)
                && Objects.equals(imaginaryTarget, that.imaginaryTarget)
                && Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realTarget, imaginaryTarget, source);
        }

        @Override
        public String toString() {
            return "FalseEdge(realTarget=" + realTarget + ", imaginaryTarget=" + imaginaryTarget + ")";
        }
    }

    final class FalseUnwind<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final BB realTarget;
        @Nullable
        private final BB unwind;
        @Nonnull
        private final MirSourceInfo source;

        public FalseUnwind(@Nonnull BB realTarget, @Nullable BB unwind, @Nonnull MirSourceInfo source) {
            this.realTarget = realTarget;
            this.unwind = unwind;
            this.source = source;
        }

        @Nonnull
        public BB getRealTarget() {
            return realTarget;
        }

        @Nullable
        public BB getUnwind() {
            return unwind;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            List<MirBasicBlock> result = new ArrayList<>();
            result.add(realTarget);
            if (unwind != null) result.add(unwind);
            return result;
        }

        @Nonnull
        public FalseUnwind<BB> copy(@Nullable BB realTarget, @Nullable BB unwind, @Nullable MirSourceInfo source) {
            return new FalseUnwind<>(
                realTarget != null ? realTarget : this.realTarget,
                unwind != null ? unwind : this.unwind,
                source != null ? source : this.source
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FalseUnwind<?> that = (FalseUnwind<?>) o;
            return Objects.equals(realTarget, that.realTarget)
                && Objects.equals(unwind, that.unwind)
                && Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realTarget, unwind, source);
        }

        @Override
        public String toString() {
            return "FalseUnwind(realTarget=" + realTarget + ", unwind=" + unwind + ")";
        }
    }

    final class Unreachable<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirSourceInfo source;

        public Unreachable(@Nonnull MirSourceInfo source) {
            this.source = source;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            return Collections.emptyList();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Unreachable<?> that = (Unreachable<?>) o;
            return Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source);
        }

        @Override
        public String toString() {
            return "Unreachable(source=" + source + ")";
        }
    }

    final class Call<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirOperand callee;
        @Nonnull
        private final List<MirOperand> args;
        @Nonnull
        private final MirPlace destination;
        @Nullable
        private final BB target;
        @Nullable
        private final BB unwind;
        private final boolean fromCall;
        @Nonnull
        private final MirSourceInfo source;

        public Call(
            @Nonnull MirOperand callee,
            @Nonnull List<MirOperand> args,
            @Nonnull MirPlace destination,
            @Nullable BB target,
            @Nullable BB unwind,
            boolean fromCall,
            @Nonnull MirSourceInfo source
        ) {
            this.callee = callee;
            this.args = args;
            this.destination = destination;
            this.target = target;
            this.unwind = unwind;
            this.fromCall = fromCall;
            this.source = source;
        }

        @Nonnull
        public MirOperand getCallee() {
            return callee;
        }

        @Nonnull
        public List<MirOperand> getArgs() {
            return args;
        }

        @Nonnull
        public MirPlace getDestination() {
            return destination;
        }

        @Nullable
        public BB getTarget() {
            return target;
        }

        @Nullable
        public BB getUnwind() {
            return unwind;
        }

        public boolean isFromCall() {
            return fromCall;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            List<MirBasicBlock> result = new ArrayList<>();
            if (target != null) result.add(target);
            if (unwind != null) result.add(unwind);
            return result;
        }

        @Nonnull
        public Call<BB> copy(
            @Nullable MirOperand callee,
            @Nullable List<MirOperand> args,
            @Nullable MirPlace destination,
            @Nullable BB target,
            @Nullable BB unwind,
            @Nullable Boolean fromCall,
            @Nullable MirSourceInfo source
        ) {
            return new Call<>(
                callee != null ? callee : this.callee,
                args != null ? args : this.args,
                destination != null ? destination : this.destination,
                target != null ? target : this.target,
                unwind != null ? unwind : this.unwind,
                fromCall != null ? fromCall : this.fromCall,
                source != null ? source : this.source
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Call<?> call = (Call<?>) o;
            return fromCall == call.fromCall
                && Objects.equals(callee, call.callee)
                && Objects.equals(args, call.args)
                && Objects.equals(destination, call.destination)
                && Objects.equals(target, call.target)
                && Objects.equals(unwind, call.unwind)
                && Objects.equals(source, call.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(callee, args, destination, target, unwind, fromCall, source);
        }

        @Override
        public String toString() {
            return "Call(callee=" + callee + ", args=" + args + ", destination=" + destination + ", target=" + target + ")";
        }
    }

    final class Drop<BB extends MirBasicBlock> implements MirTerminator<BB> {
        @Nonnull
        private final MirPlace place;
        @Nonnull
        private final BB target;
        @Nullable
        private final BB unwind;
        @Nonnull
        private final MirSourceInfo source;

        public Drop(@Nonnull MirPlace place, @Nonnull BB target, @Nullable BB unwind, @Nonnull MirSourceInfo source) {
            this.place = place;
            this.target = target;
            this.unwind = unwind;
            this.source = source;
        }

        @Nonnull
        public MirPlace getPlace() {
            return place;
        }

        @Nonnull
        public BB getTarget() {
            return target;
        }

        @Nullable
        public BB getUnwind() {
            return unwind;
        }

        @Override
        @Nonnull
        public MirSourceInfo getSource() {
            return source;
        }

        @Override
        @Nonnull
        public List<MirBasicBlock> getSuccessors() {
            List<MirBasicBlock> result = new ArrayList<>();
            result.add(target);
            if (unwind != null) result.add(unwind);
            return result;
        }

        @Nonnull
        public Drop<BB> copy(@Nullable MirPlace place, @Nullable BB target, @Nullable BB unwind, @Nullable MirSourceInfo source) {
            return new Drop<>(
                place != null ? place : this.place,
                target != null ? target : this.target,
                unwind != null ? unwind : this.unwind,
                source != null ? source : this.source
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Drop<?> drop = (Drop<?>) o;
            return Objects.equals(place, drop.place)
                && Objects.equals(target, drop.target)
                && Objects.equals(unwind, drop.unwind)
                && Objects.equals(source, drop.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(place, target, unwind, source);
        }

        @Override
        public String toString() {
            return "Drop(place=" + place + ", target=" + target + ", unwind=" + unwind + ")";
        }
    }
}
