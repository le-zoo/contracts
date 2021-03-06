package fr.lezoo.contracts.contract.permanent;

import fr.lezoo.contracts.Contracts;
import fr.lezoo.contracts.contract.ContractState;
import fr.lezoo.contracts.contract.PaymentType;
import fr.lezoo.contracts.utils.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * Here the "employee" is the lender and the "employer" the borrower because it is the lender who first create
 * The paiement info correspond to the initial paiement being made
 */
public class LendingContract extends PermanentContract {
    //period between each refund in hours
    //numberRefunds is the number of time there need to be a refund and refunds made correspond to the refunds already made
    private int interestPeriod, interestRate, numberRefunds;
    private int refundsMade;
    private double moneyPerRefund;
    private BukkitRunnable runnable;


    public LendingContract(ConfigurationSection section) {
        super(section);
        interestPeriod = section.getInt("period");
        interestRate = section.getInt("interest-rate");
        numberRefunds = section.getInt("number-refunds");
        refundsMade = section.getInt("refunds-made");
        //Calculates the money that needs to be given per refund
        moneyPerRefund = (paymentInfo.getAmount() * (1 + ((double) interestRate) / 100)) / numberRefunds;
        startRunnable();
    }

    @Override
    public void createContract() {
        moneyPerRefund = (paymentInfo.getAmount() * (1 + ((double) interestRate) / 100)) / numberRefunds;
    }


    public LendingContract(UUID employer) {
        super(employer);

        addParameter("interest-period", (p, str) -> {
            try {
                interestPeriod = Integer.parseInt(str);
            } catch (Exception e) {
                Message.NOT_VALID_INTEGER.format("input", str).send(p);
            }
        });
        addParameter("interest-rate", (p, str) -> {
            try {
                interestRate = Integer.parseInt(str);
            } catch (Exception e) {
                Message.NOT_VALID_INTEGER.format("input", str).send(p);
            }
        });
        addParameter("number-refunds", (p, str) -> {
            try {
                numberRefunds = Integer.parseInt(str);
            } catch (Exception e) {
                Message.NOT_VALID_INTEGER.format("input", str).send(p);
            }
        });

        //At the beginning
        refundsMade = 0;
        startRunnable();

    }

    public void startRunnable() {
        runnable = new BukkitRunnable() {

            @Override
            public void run() {

                //employer lender so it the employer who gives money to the employee
                if (paymentInfo.getType() == PaymentType.MONEY) {
                    if (Contracts.plugin.economy.getBalance(Bukkit.getOfflinePlayer(employer)) > moneyPerRefund) {
                        Contracts.plugin.economy.withdrawPlayer(Bukkit.getOfflinePlayer(employer), moneyPerRefund);
                        Contracts.plugin.economy.depositPlayer(Bukkit.getOfflinePlayer(employee), moneyPerRefund);
                        refundsMade++;
                    }
                    //If the player can't refund than the contract is cancelled and a middle man will come
                    else
                        callDispute();
                }
                if (refundsMade >= numberRefunds) {
                    changeContractState(ContractState.FULFILLED);
                }
            }
        };
        //1 hour =60*60*20 ticks
        runnable.runTaskTimer(Contracts.plugin, 0, interestPeriod * 60 * 60 * 20);
    }

}
