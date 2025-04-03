// 40 mins
// 227. Basic Calculator
/* 
Given a string s which represents an expression, 
evaluate this expression and return its value. 

The integer division should truncate toward zero.
You may assume that the given expression is always valid. 
All intermediate results will be in the range of [-2^31, 2^31 - 1].

Note: You are not allowed to use any built-in function which evaluates strings as mathematical expressions, such as eval().
Example 1:
Input: s = "3+2*2"
Output: 7


Example 2:
Input: s = " 3/2 "
Output: 1

Example 3:
Input: s = " 3+5 / 2 "
Output: 5


skip spaces
no deciamls
no commas in large int values

rules pemdas


delimiters = "/*+-" operation
Input: s = " 3+5 / 2 "
Input: s = " 3+5/2*2 " output = 7
Input: s = " 3+4 " output = 7
Input: s = " 7 " output = 7

 stack = [+,/,*] = Left-Right Iteration
 +-stack = [+] = Right to Left 
 /*stack = [*,/] = Right to Left 
 number = [2,2,5,3]
 
 */
function removeSpaces(string) {}
function calculateExpression(expression) {
  //let cleanedExpression = removeSpaces(expression);
  let indexOfOperations = [];
  let prevNumber = 0;
  let nextNumber = 0;
  let newString = "";
  for (let tokenIndex = 0; tokenIndex < expression.length; tokenIndex++) {
    let nextNumberEndIndex = 0;
    let prevNumberStartIndex = 0;
    if (expression[tokenIndex] in ["+", "-"]) {
      indexOfOperations.push(tokenIndex);
    }
    if (expression[tokenIndex] in ["*", "/"]) {
      prevNumberStartIndex = indexOfOperations.pop();
      prevNumber = Number(
        expression.substring(prevNumberStartIndex + 1, tokenIndex)
      );
        for (// 234 + 211 * 2 
                //        ^   
        let subtokenIndex = tokenIndex;
        subtokenIndex < expression.length;
        subtokenIndex++
      ) {
        if (expression[tokenIndex] in ["+", "-"]) {
          nextNumberEndIndex = subtokenIndex;
          break;
        }
        nextNumberEndIndex = expression.length;
      }

      nextNumber = Number(
        expression.substring(tokenIndex + 1, nextNumberEndIndex)
      );
      if (expression[tokenIndex] == "*") {
        newString =
          newString.substring(0, prevNumberStartIndex - 1) +
          String(prevNumber * nextNumber);
        continue;
      }
      if (expression[tokenIndex] == "/") {
        newString =
          newString.substring(0, prevNumberStartIndex - 1) + // getting non affected numbers  // leetcode ranked high 
          String(Math.floor(prevNumber / nextNumber)); /// Calculated Number
        continue;
      }
    }
    newString += expression[tokenIndex];
  }

  return newString;
}

console.log(calculateExpression("3+5*5/5"));
