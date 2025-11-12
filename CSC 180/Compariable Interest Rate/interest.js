document.getElementById('interestForm').addEventListener('submit', function(event) {
    event.preventDefault();

    const principal = parseFloat(document.getElementById('principal').value);
    const monthlyContribution = parseFloat(document.getElementById('monthlyContribution').value);
    const rate = parseFloat(document.getElementById('rate').value) / 100;
    const years = parseInt(document.getElementById('years').value);
    const taxRate = parseFloat(document.getElementById('taxRate').value) / 100;
    const resultsDiv = document.getElementById('results');

    const r_monthly = rate / 12;
    const n_months = years * 12;

    if (rate === 0) {
        const totalInvested = principal + (monthlyContribution * n_months);
        resultsDiv.innerHTML = `
            <p>With a 0% interest rate, no gains are made. The final amount is the total invested.</p>
            <h4>Final Amount: \$${totalInvested.toFixed(2)}</h4>
        `;
        return;
    }

    // Scenario 1: Tax-Exempt until the end (with monthly contributions)
    const fvPrincipalExempt = principal * Math.pow(1 + r_monthly, n_months);
    const fvContributionsExempt = monthlyContribution * ((Math.pow(1 + r_monthly, n_months) - 1) / r_monthly);
    const futureValueExempt = fvPrincipalExempt + fvContributionsExempt;
    
    const totalInvested = principal + (monthlyContribution * n_months);
    const gainExempt = futureValueExempt - totalInvested;
    const taxesExempt = gainExempt > 0 ? gainExempt * taxRate : 0;
    const finalAmountExempt = futureValueExempt - taxesExempt;

    // Scenario 2: Taxed annually on gains (with monthly contributions)
    let finalAmountTaxedAnnually = principal;
    const fv_monthly_contrib_1_year = monthlyContribution * ((Math.pow(1 + r_monthly, 12) - 1) / r_monthly);

    for (let i = 0; i < years; i++) {
        const investedAtStartOfYear = finalAmountTaxedAnnually;
        
        // Corrected: Compound the previous year's amount monthly
        const valueFromPreviousYear = investedAtStartOfYear * Math.pow(1 + r_monthly, 12);
        
        const endOfYearValue_preTax = valueFromPreviousYear + fv_monthly_contrib_1_year;
        
        const totalInvestedThisYear = investedAtStartOfYear + (monthlyContribution * 12);
        const gainThisYear = endOfYearValue_preTax - totalInvestedThisYear;
        const taxThisYear = gainThisYear > 0 ? gainThisYear * taxRate : 0;
        
        finalAmountTaxedAnnually = endOfYearValue_preTax - taxThisYear;
    }

    resultsDiv.innerHTML = `
        <h4>Scenario 1: Tax-Exempt (Taxes Paid at End)</h4>
        <p>Future Value (before tax): \$${futureValueExempt.toFixed(2)}</p>
        <p>Total Invested: \$${totalInvested.toFixed(2)}</p>
        <p>Total Gain: \$${gainExempt.toFixed(2)}</p>
        <p>Taxes Due at End: \$${taxesExempt.toFixed(2)}</p>
        <p><strong>Final Amount: \$${finalAmountExempt.toFixed(2)}</strong></p>
        <hr>
        <h4>Scenario 2: Taxed Annually on Gains</h4>
        <p><strong>Final Amount: \$${finalAmountTaxedAnnually.toFixed(2)}</strong></p>
        <hr>
        <h4>Comparison</h4>
        <p>The tax-exempt strategy results in <strong>\$${(finalAmountExempt - finalAmountTaxedAnnually).toFixed(2)}</strong> more after ${years} years.</p>
    `;
});
