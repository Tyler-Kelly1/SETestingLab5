package SpellcastingTest;

import Spellcasting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.provider.CsvFileSource;

public class SpellcastingTest {

    @Test
    public void test_rest_whenExhausted_resetsStanceToIdle(){


        // Intialized the wizard
        Wizard wizard = new Wizard(1, 10);

        // Wizard starts by channeling for surge
        wizard.beginChanneling();

        // Trigger spell surge three times leading to EXHAUSTED State
        wizard.spellSurge(1);
        wizard.spellSurge(1);
        wizard.spellSurge(1);
        wizard.spellSurge(1);


        // Call rest() to reset the wizard back to IDLE and 600 mana
        wizard.rest();

        assertAll("Wizard is reset to idle properly",
                () -> assertEquals(Wizard.Stance.IDLE, wizard.getCurrentStance()),
                () -> assertEquals(wizard.getManaStore(), 600)
        );

    }

    @Test
    public void test_surging_whenNew(){

        Wizard wizard = new Wizard(1, 10);

        try{
            wizard.spellSurge(1);
        }
        catch (InvalidStateException e){
            assertEquals("Must begin channeling before surging.", e.getMessage());
        }

    }

    @ParameterizedTest
    @CsvFileSource(resources = "/test_manaPositive_whenSurging_data.csv", numLinesToSkip = 1)
    public void test_manaPositive_whenSurging(int surgeMana1, int surgeMana2, int spellMana1, int spellMana2){

        Wizard wizard = new Wizard(1, 10);

        // Set wizard to channeling for surge
        wizard.beginChanneling();

        try{

            wizard.spellSurge(surgeMana1);
            wizard.spellSurge(surgeMana2);
            wizard.castSpell(spellMana1);
            wizard.castSpell(spellMana2);

        }
        catch (Exception e){
            assertEquals("Insufficient mana to cast", e.getMessage());
        }

        assert false;
    }

    @Test
    public void test_manaStore_whenSpellCaster(){

        Spellcaster wizard = new Archmage(10, 100);

        // Activate the barrier

        // However, a 'spellcaster' cannot invoke togglebarrier must use a cast
        ((Archmage) wizard).toggleBarrier();

        // Damage is higher than the 600 mana limit

        try{
            wizard.receiveDamage(650);
        }
        catch (ManaInvestedException e){
            assertEquals("Insufficient mana to transfer.", e.getMessage());
        }

        assert false;

    }

    @Test
    public void test_spellSurge_superOverride(){

        Archmage wizard = new Archmage(10, 100);

        // Begin channeling to be able to surge
        wizard.beginChanneling();

        // Spell surge excedding 200 power should call this.rest
        wizard.spellSurge(100);

        // This should result in a state of rest
        assertEquals(Wizard.Stance.IDLE, wizard.getCurrentStance());
        assertEquals(1, wizard.consecutiveSurges);

    }

    @Test
    public void test_wizardArchmage_consistent(){

        Wizard wizard = new Wizard(10, 100);
        Archmage archmage = new Archmage(10, 100);

        Object target = new Object();

        // Test operations that should be identical
        assertAll("Archmage and Wizard should have the same state",
                () -> assertEquals(wizard.getManaStore(), archmage.getManaStore()),
                () -> assertEquals(wizard.getHp(), archmage.getHp()),
                () -> assertEquals(wizard.getLevel(), archmage.getLevel()),
                () -> assertEquals(wizard.castSpell(target), archmage.castSpell(target))
        );

    }

    @Test
    public void transferMana_deductsExactAmountFromSender() {

        // Setup two wizards.
        Wizard sender = new Wizard(10, 100);
        Wizard recipient = new Wizard(10, 100);

        int initialSenderMana = sender.getManaStore();
        int initialRecipientMana = recipient.getManaStore();
        int amountToTransfer = 100;
        int priorityTier = 1;

        // Transfer 100 mana from sender to recipient.
        sender.transferMana(recipient, amountToTransfer, priorityTier);

        // Catch the homogeneous type parameter swap defect
        assertAll("Mana transfer balances should update exactly by the transfer amount",
                () -> assertEquals(initialSenderMana - amountToTransfer, sender.getManaStore(),
                        "Sender should lose exactly 100 mana. The parameter swap defect deducts priorityTier (1) instead."),
                () -> assertEquals(initialRecipientMana + amountToTransfer, recipient.getManaStore(),
                        "Recipient should gain exactly 100 mana.")
        );
    }

    @Test
    public void test_castSpell_staticOverloadResolution() {
        Wizard wizard = new Wizard(10, 100);

        // Create a specific Spell instance requiring a base mana cost of 20 and min level of 5
        Spell fireballSpell = new Spell("Fireball", 20, 5);

        // Same spell using gen obj
        Object spellAsObject = fireballSpell;

        // This execution path deducts mana and returns baseManaCost * 2 (20 * 2 = 40)
        int strongTypeResult = wizard.castSpell(fireballSpell);

        // This execution path acts as a fallback overload returning a flat damage of 10
        int weakTypeResult = wizard.castSpell(spellAsObject);

        assertAll("Compile-time declared types change execution paths for overloaded methods",
                () -> assertEquals(40, strongTypeResult, "Strongly typed Spell reference correctly routes to castSpell(Spell)."),
                () -> assertEquals(10, weakTypeResult, "Object reference incorrectly resolves at compile time to the castSpell(Object) fallback.")
        );

        // Wrapper assertion enforcing safe message handling via dynamic checking
        assertEquals(40, safeCastMessageDispatcher(wizard, spellAsObject),
                "The dispatcher should dynamically identify the underlying Spell instance and invoke the correct overload.");
    }

    private int safeCastMessageDispatcher(Wizard wizard, Object target) {
        if (target instanceof Spell) {
            // Explicitly cast to utilize the correct static overload
            return wizard.castSpell((Spell) target);
        }
        // Fallback for non-spell objects
        return wizard.castSpell(target);
    }


}
