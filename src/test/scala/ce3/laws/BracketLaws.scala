/*
 * Copyright 2019 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ce3
package laws

import cats.MonadError
import cats.implicits._
import cats.laws.MonadErrorLaws

trait BracketLaws[F[_], E] extends MonadErrorLaws[F, E] {

  implicit val F: Bracket[F, E]

  import F.CaseInstance

  def bracketPureCoherence[A, B](acq: F[A], f: A => B, release: (A, F.Case[B]) => F[Unit]) =
    F.bracketCase(acq)(a => F.pure(f(a)))(release) <-> acq.flatMap(a => release(a, CaseInstance.pure(f(a))).as(f(a)))

  def bracketErrorCoherence[A](acq: F[A], f: A => E, release: (A, F.Case[Unit]) => F[Unit]) =
    F.bracketCase(acq)(a => F.raiseError[Unit](f(a)))(release) <-> acq.flatMap(a => release(a, CaseInstance.raiseError[Unit](f(a))) *> F.raiseError[Unit](f(a)))

  def bracketFlatMapAttemptIdentity[A, B](acq: F[A], f: A => F[B], release: A => F[Unit]) = {
    val result = F.bracketCase(acq)(f)((a, _) => release(a))
    val expect = acq.flatMap(a => f(a).attempt <* release(a)).rethrow
    result <-> expect
  }

  def bracketErrorIdentity[A, B](e: E, f: A => F[B], release: F[Unit]) =
    F.bracketCase(F.raiseError[A](e))(f)((_, _) => release) <-> F.raiseError[B](e)
}

object BracketLaws {
  def apply[F[_], E](implicit F0: Bracket[F, E]): BracketLaws[F, E] { val F: F0.type } =
    new BracketLaws[F, E] { val F: F0.type = F0 }
}
