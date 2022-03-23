package part1recap

object ThreadModelLimitations extends App {

  class BankAccount(private var amount: Int) {
    override def toString(): String = s"$amount"

    def withdraw(money: Int) = synchronized { this.amount -= money }
    def deposit(money: Int) = synchronized { this.amount += money }

    def getAmount: Int = this.amount
  }

  val account = new BankAccount(2000)
  val depositThreads =
    (1 to 1000).map(_ => new Thread(() => account.deposit(1)))
  val withdrawThreads =
    (1 to 1000).map(_ => new Thread(() => account.withdraw(1)))

  (depositThreads ++ withdrawThreads).foreach(_.start())

  Thread.sleep(1000)

  println(account.getAmount)
}
