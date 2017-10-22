package visitors;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

import business.Arreglo;
import business.Constants;
import business.Error;
import business.Subrutina;
import business.Variable;
import classes.tclBaseVisitor;
import classes.tclParser.AgrupContext;
import classes.tclParser.AsignacionContext;
import classes.tclParser.DeclaracionContext;
import classes.tclParser.Declaracion_funcionContext;
import classes.tclParser.Exp_addContext;
import classes.tclParser.Exp_andContext;
import classes.tclParser.Exp_igContext;
import classes.tclParser.Exp_mulContext;
import classes.tclParser.Exp_orContext;
import classes.tclParser.Exp_potContext;
import classes.tclParser.Exp_relContext;
import classes.tclParser.Exp_unaContext;
import classes.tclParser.GetsContext;
import classes.tclParser.IndiceContext;
import classes.tclParser.InicioContext;
import classes.tclParser.Param_funcContext;
import classes.tclParser.PutsContext;
import classes.tclParser.ValorContext;

public class VisitorTCL<T> extends tclBaseVisitor<T> {
	
	
	ArrayList<Hashtable<String, Object>> tables = new ArrayList<>();
	Hashtable<String, Subrutina> tableFunctions = new Hashtable<>();
	
	@Override
	public T visitInicio(InicioContext ctx) {		
		tables.add(new Hashtable<>());
		return super.visitInicio(ctx);
	}
	
	@Override
	public T visitDeclaracion_funcion(Declaracion_funcionContext ctx) {		
		if(ctx.IDENTIFICADOR() != null){
			
			String nameId = ctx.IDENTIFICADOR().getText();
			// Si ya existia una funcion con el mismo nombre
			if(tableFunctions.containsKey(nameId)){
				String msj = Error.repeatedFunction(nameId);
				int line = ctx.IDENTIFICADOR().getSymbol().getLine();
				int col = ctx.IDENTIFICADOR().getSymbol().getCharPositionInLine()+1;
				Error.printError(msj, line, col);		
				return null;
			} else {
				tableFunctions.put(nameId, new Subrutina(ctx.cuerpo_funcion()));
				
				
				/*
				 * Hay que obetener los argumentos, y ponerle esos argumentos a la funcion
				 */			
			}
		}
		return super.visitDeclaracion_funcion(ctx);
	}
	
	
	@Override
	public T visitDeclaracion(DeclaracionContext ctx) {
		String nameVar = ctx.IDENTIFICADOR().getText();
		Variable indice = null;
		if(ctx.indice().val_indice() != null){ // Verifica que haya un indice
			indice = (Variable) visitIndice(ctx.indice());
		}
		
		Object temp = valueID(nameVar);
		Variable newValue = (Variable)visitAsignacion(ctx.asignacion());
		Hashtable<String, Object> tempTable;
		if(temp == null){ // Si se cumple la variable no exist�a
			if(indice != null){ // si se cumple se esta creando un nuevo arreglo
				tempTable = tables.get(tables.size()-1);
				Arreglo newArreglo = new Arreglo();
				newArreglo.insertIndice(indice.getValor(), newValue);
				tempTable.put(nameVar, newArreglo);				
			} else { // si no se esta creando una nueva variable
				tempTable = tables.get(tables.size()-1);
				tempTable.put(nameVar, newValue);
			}
		} else {
			if(indice != null){ // si se cumple se puede actualizar o un nuevo indice
				Arreglo arr = (Arreglo)temp;
				if(arr.containsKey(indice.getValor())){ // se actualiza valor de indice
					arr.updateIndex(indice.getValor(), newValue);
				} else { // si no se crea un nuevo indice
					arr.insertIndice(indice.getValor(), newValue);
				}				
			} else {
				// se cambia a variable si la que existia era arreglo
				if(temp.getClass().getName().equals("business.Arreglo")){ 
					tempTable = tables.get(tables.size()-1);
					tempTable.remove(nameVar);
					tempTable.put(nameVar, newValue);
				} else { // se actualiza el valor de la variable
					Variable cur = (Variable) temp;
					cur.setValor(newValue.getValor());
				}
			}
		}
		
		return null;
	}	
	
	@Override
	public T visitPuts(PutsContext ctx) {
		Variable val;
		val = (Variable)visitAsignacion(ctx.asignacion());
		System.out.println(val.getValor());
		return null;
	}
	
	@Override
	public T visitAsignacion(AsignacionContext ctx) {
		if(ctx.valor() != null){ // Es un valor string, double o int
			return visitValor(ctx.valor());
		} else if(ctx.agrup() != null){ // Es una agrupacion por cor. cuadrados
			return visitAgrup(ctx.agrup());
		} else { // Si no es un identificador
			String nameVar = ctx.IDENTIFICADOR().getText();			
			Variable indice = null;
			if(ctx.indice() != null){ // Se mira si existe un indice
				indice = (Variable)visitIndice(ctx.indice());
			}
			
			Object temp = valueID(nameVar);
			if(temp == null){ // Si se cumple, la cariable no existe
				String msj = Error.variableNotDeclared(nameVar);
				int line = ctx.IDENTIFICADOR().getSymbol().getLine();
				int col = ctx.IDENTIFICADOR().getSymbol().getCharPositionInLine();
				Error.printError(msj, line, col);	
				return null;
			} else {
				if(indice != null){ // Si existe algun indice				

					// Es una variable y se esta pasando como arreglo -> ERROR
					if(temp.getClass().getName().equals("business.Variable")){ 
						String msj = Error.variableNotArray(nameVar);
						int line = ctx.IDENTIFICADOR().getSymbol().getLine();
						int col = ctx.IDENTIFICADOR().getSymbol().getCharPositionInLine();
						Error.printError(msj, line, col);
						return null;
					}
					
					Arreglo arr = (Arreglo) temp;
					// Se mira si el indice existe, si no, es un error
					if(arr.containsKey(indice.getValor())){						
						return (T) arr.getValue(indice.getValor());
					} else {
						String msj = Error.arrayWithoutKey(nameVar, indice.valorToString()); 
						int line = ctx.IDENTIFICADOR().getSymbol().getLine();
						int col = ctx.IDENTIFICADOR().getSymbol().getCharPositionInLine();
						Error.printError(msj, line, col);	
						return null;
					}
				} else {
					// Si se esta llamando a una variable pero es un arreglo -> ERROR
					if(temp.getClass().getName().equals("business.Arreglo")){
						String msj = Error.variableIsArray(nameVar);
						int line = ctx.IDENTIFICADOR().getSymbol().getLine();
						int col = ctx.IDENTIFICADOR().getSymbol().getCharPositionInLine();
						Error.printError(msj, line, col);
						return null;
					} else 
						return (T) (Variable) temp;									
				}	
			}
				
		}
	}
		
	@Override
	public T visitIndice(IndiceContext ctx) {
		if(ctx.val_indice() != null){ // Si se tiene un valor entre los parentesis		
			if(ctx.val_indice().valor() != null){ // Si es de tipo valor el indice
				return (T) visitValor(ctx.val_indice().valor());
			} else { // Si no, es de tipo agrupacion
				return (T) visitAgrup(ctx.val_indice().agrup());
			}		
		}
		return null;
	}
	
	@Override
	public T visitParam_func(Param_funcContext ctx) {
		List<Variable> params = new ArrayList<>();
		if(ctx.aux_param() != null){
			if(ctx.aux_param().asignacion() !=null){
				params.add((Variable) visitAsignacion(ctx.aux_param().asignacion()));
			} else if(ctx.aux_param().expr() != null) {
				params.add((Variable) visitExpresion(ctx.aux_param().expr().expresion()));
				
			}
			params.addAll((List<Variable>) visitParam_func(ctx.aux_param().param_func()));
			
		}
		return (T) params;
	}
	
	@Override
	public T visitAgrup(AgrupContext ctx) {
		if(ctx.aux_agrup().expr() != null){ // Si dentro de la agrupacion es expr
			return (T) visitExpresion(ctx.aux_agrup().expr().expresion());
		} else if(ctx.aux_agrup().param_func() != null){ // si dentro de agrup hay llamado a funcion
			String nameFunc = ctx.aux_agrup().IDENTIFICADOR().getText();
			
			List<Variable> params = (List<Variable>)visitParam_func(ctx.aux_agrup().param_func());
			
			
			/*
			 * En esta parte hay que verificar primero que el numero de parametros sea correcto
			 * Luego hay que pasar a ejecutar la funcion y retornar ese valor devuelto
			 */
			
			
			return (T)new Variable(Constants.INT, 0);
		} else if(ctx.aux_agrup().aux_array() != null){ // Si hay dentro una accion de array
			String command = ctx.aux_agrup().aux_array().getStart().getText();
			String nameId = ctx.aux_agrup().aux_array().IDENTIFICADOR().getText();
			Object temp = valueID(nameId);
			
			if(command.equals("size")){ // Realiza la accion de 'size'
				if(temp == null){ // Si la variable no existe -> ERROR
					String msj = Error.variableNotDeclared(nameId);
					int line = ctx.aux_agrup().aux_array().IDENTIFICADOR().getSymbol().getLine();
					int col = ctx.aux_agrup().aux_array().IDENTIFICADOR().getSymbol().getCharPositionInLine()+1;
					Error.printError(msj, line, col);
					return null;
				}
				
				// Si variable no es un arreglo -> ERROR
				if(!temp.getClass().getName().equals("business.Arreglo")){
					String msj = Error.variableNotArray(nameId);
					int line = ctx.aux_agrup().aux_array().IDENTIFICADOR().getSymbol().getLine();
					int col = ctx.aux_agrup().aux_array().IDENTIFICADOR().getSymbol().getCharPositionInLine()+1;
					Error.printError(msj, line, col);
					return null;
				} else {					
					Arreglo arr = (Arreglo) temp;
					return (T) new Variable(Constants.INT, arr.getSize());			
				}
			} else { // es la accion de 'exists'
				// variable existe y es un arreglo
				if(temp != null && temp.getClass().getName().equals("business.Arreglo")){
					return (T) new Variable(Constants.INT, 1);
				} else {
					return (T) new Variable(Constants.INT, 0);
				}
			}
			
		} else if(ctx.aux_agrup().gets() != null){ // Se va a gets
			return visitGets(ctx.aux_agrup().gets());
		}
		return null;
	}
	
	@Override
	public T visitValor(ValorContext ctx) {
		if(ctx.VALOR_DOUBLE() != null){ // Mira si es un double
			return (T) new Variable(Constants.DOUBLE, Double.parseDouble(ctx.VALOR_DOUBLE().getText()));
		} else if(ctx.VALOR_ENTERO() != null){ // Mira si es un Entero
			return (T) new Variable(Constants.INT, Integer.parseInt(ctx.VALOR_ENTERO().getText()));
		}
		
		// Hace la parte del String - Hay que mejorar, funciona regular
		
		String temp = ctx.VALOR_STRING().getText();
		String []words = temp.substring(1, temp.length()-1).split(" ");
		StringBuilder result = new StringBuilder("");
		temp = "";
		Object res;
		int colTemp = 0;
		for(String word : words){
			if(!word.isEmpty() && word.charAt(0) == '$'){
				temp = word.substring(1);
				res = valueID(temp);
				if(res != null ){
					Variable val = (Variable) res;
					result.append(val.getValor().toString() + " ");
				} else {
					String msj = Error.variableNotDeclared(temp);
					int line = ctx.VALOR_STRING().getSymbol().getLine();
					int col = ctx.VALOR_STRING().getSymbol().getCharPositionInLine();
					col += 2 + colTemp;
					Error.printError(msj, line, col);					
				}
			} else {
				result.append((String)word + " ");
			}
			colTemp += word.length()+1;
		}
		return (T) new Variable(Constants.STRING, result.toString());
	}	
	
	
	@Override
	public T visitGets(GetsContext ctx) {
		Scanner input = new Scanner(System.in);
		String result = input.nextLine();
		input.close();
		
		if(result.matches("'-'?[0-9]+")){ // si hace match con la regex es un INT
			return (T) new Variable(Constants.INT, Integer.parseInt(result));
		} else if(result.matches("-?[0-9]+.[0-9]+")){ // si no, es un Double
			return (T) new Variable(Constants.DOUBLE, Double.parseDouble(result));
		} else { // si no es un string
			return (T) new Variable(Constants.STRING, result);
		}
	}
	
	
	/*
	 * Funcion se encarga de mirar si la variable existe en alguna de las tablas
	 * Si no existe retorna null y si existe retorna la Variable que corresponda
	 */
	public T valueID(String id){ 
		for(Hashtable<String, Object> table : tables){
			if(table.containsKey(id)){
				return (T) table.get(id);
			}
		}
		return null;
	}
	
	
    @Override
    public T visitExp_or(Exp_orContext ctx) {
        Variable var1, var2 = (Variable) visitExp_and(ctx.exp_and()), var3;
        if (ctx.exp_or() != null) {
            var1 = (Variable) visitExp_or(ctx.exp_or());
            var3 = new Variable(Constants.INT, null);
            if (var1.getTipo() == Constants.INT && var2.getTipo() == Constants.INT) {
                var3.setValor((!((Integer) var1.getValor()).equals(0) || !((Integer) var2.getValor()).equals(0)) ? (Object) 1 : (Object) 0);
            } else;
// Error, se esperaba ENTERO                        
            return (T) var3;
        }
        return (T) var2;
    }

    @Override
    public T visitExp_and(Exp_andContext ctx) {
        Variable var1, var2 = (Variable) visitExp_ig(ctx.exp_ig()), var3;
        if (ctx.exp_and() != null) {
            var1 = (Variable) visitExp_and(ctx.exp_and());
            var3 = new Variable(Constants.INT, null);
            if (var1.getTipo() == Constants.INT && var2.getTipo() == Constants.INT) {
                var3.setValor((!((Integer) var1.getValor()).equals(0) && !((Integer) var2.getValor()).equals(0)) ? (Object) 1 : (Object) 0);
            } else;
// Error, se esperaba ENTERO                        
            return (T) var3;
        }
        return (T) var2;
    }

    @Override
    public T visitExp_ig(Exp_igContext ctx) {
        Variable var1, var2 = (Variable) visitExp_rel(ctx.exp_rel()), var3;
        if (ctx.exp_ig() != null) {
            var1 = (Variable) visitExp_ig(ctx.exp_ig());

            var3 = new Variable(Constants.INT, null);
            String op = ctx.OP_IG().getText();
            if (op.equals("ne") || op.equals("eq")) {
                if (var1.getTipo() == Constants.STRING && var2.getTipo() == Constants.STRING) {
                    if (op.equals("eq")) {
                        var3.setValor((((String) var1.getValor()).equals((String) var2.getValor())) ? (Object) 1 : (Object) 0);
                    } else {
                        var3.setValor((((String) var1.getValor()).equals((String) var2.getValor())) ? (Object) 0 : (Object) 1);
                    }
                } else;
// Error, se esperaba Constants.STRING
            } else {
                if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING); // Error, se esperaba tipo numérico, se encontró Constants.STRING
                else if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                    if (op.equals("==")) {
                        var3.setValor((((Double) var1.getValor()).equals((Double) var2.getValor())) ? (Object) 1 : (Object) 0);
                    } else {
                        var3.setValor((((Double) var1.getValor()).equals((Double) var2.getValor())) ? (Object) 0 : (Object) 1);
                    }
                } else if (op.equals("==")) {
                    var3.setValor((((Integer) var1.getValor()).equals((Integer) var2.getValor())) ? (Object) 1 : (Object) 0);
                } else {
                    var3.setValor((((Integer) var1.getValor()).equals((Integer) var2.getValor())) ? (Object) 0 : (Object) 1);
                }
            }
            return (T) var3;
        }
        return (T) var2;
    }

    @Override
    public T visitExp_rel(Exp_relContext ctx) {
        Variable var1, var2 = (Variable) visitExp_add(ctx.exp_add()), var3;
        if (ctx.exp_rel() != null) {
            var1 = (Variable) visitExp_rel(ctx.exp_rel());

            var3 = new Variable(Constants.INT, null);
            String op = ctx.OP_REL().getText();
            if (op.equals(">=")) {
                if (var1.getTipo() == Constants.STRING && var2.getTipo() == Constants.STRING) {
                    var3.setValor((0 < ((String) var1.getValor()).compareTo((String) var2.getValor())) ? (Object) 0 : (Object) 1);
                } else if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING); // Error, String no se puede comparar con otro tipo de dato
                else if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                    var3.setValor(((Double) var1.getValor() >= (Double) var2.getValor()) ? (Object) 1 : (Object) 0);
                } else {
                    var3.setValor(((Integer) var1.getValor() >= (Integer) var2.getValor()) ? (Object) 1 : (Object) 0);
                }
            } else if (op.equals("<=")) {
                if (var1.getTipo() == Constants.STRING && var2.getTipo() == Constants.STRING) {
                    var3.setValor((0 > ((String) var1.getValor()).compareTo((String) var2.getValor())) ? (Object) 0 : (Object) 1);
                } else if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING); // Error, String no se puede comparar con otro tipo de dato
                else if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                    var3.setValor(((Double) var1.getValor() <= (Double) var2.getValor()) ? (Object) 1 : (Object) 0);
                } else {
                    var3.setValor(((Integer) var1.getValor() <= (Integer) var2.getValor()) ? (Object) 1 : (Object) 0);
                }
            } else if (op.equals("<")) {
                if (var1.getTipo() == Constants.STRING && var2.getTipo() == Constants.STRING) {
                    var3.setValor((0 < ((String) var1.getValor()).compareTo((String) var2.getValor())) ? (Object) 1 : (Object) 0);
                } else if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING); // Error, String no se puede comparar con otro tipo de dato
                else if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                    var3.setValor(((Double) var1.getValor() < (Double) var2.getValor()) ? (Object) 1 : (Object) 0);
                } else {
                    var3.setValor(((Integer) var1.getValor() < (Integer) var2.getValor()) ? (Object) 1 : (Object) 0);
                }
            } else if (op.equals(">")) {
                if (var1.getTipo() == Constants.STRING && var2.getTipo() == Constants.STRING) {
                    var3.setValor((0 > ((String) var1.getValor()).compareTo((String) var2.getValor())) ? (Object) 1 : (Object) 0);
                } else if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING); // Error, String no se puede comparar con otro tipo de dato
                else if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                    var3.setValor(((Double) var1.getValor() > (Double) var2.getValor()) ? (Object) 1 : (Object) 0);
                } else {
                    var3.setValor(((Integer) var1.getValor() > (Integer) var2.getValor()) ? (Object) 1 : (Object) 0);
                }
            }
            return (T) var3;
        }
        return (T) var2;
    }

    @Override
    public T visitExp_add(Exp_addContext ctx) {
        Variable var1, var2 = (Variable) visitExp_mul(ctx.exp_mul()), var3;
        if (ctx.exp_add() != null) {
            var1 = (Variable) visitExp_add(ctx.exp_add());;

//  Error, se encontró Constants.STRING, se esperaba valor numérico
            if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING);

            var3 = new Variable(-1, null);
            char op = ctx.OP_ADD().getText().charAt(0);
            if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                if (op == '+') {
                    var3.setValor((Object) ((Double) var1.getValor() + (Double) var2.getValor()));
                } else {
                    var3.setValor((Object) ((Double) var1.getValor() - (Double) var2.getValor()));
                }
                var3.setTipo(Constants.DOUBLE);
            } else {
                if (op == '+') {
                    var3.setValor((Object) ((Integer) var1.getValor() + (Integer) var2.getValor()));
                } else {
                    var3.setValor((Object) ((Integer) var1.getValor() - (Integer) var2.getValor()));
                }
                var3.setTipo(Constants.INT);
            }
            return (T) var3;
        }
        return (T) var2;
    }

    @Override
    public T visitExp_mul(Exp_mulContext ctx) {
        Variable var1, var2 = (Variable) visitExp_pot(ctx.exp_pot()), var3;
        if (ctx.exp_mul() != null) {
            var1 = (Variable) visitExp_mul(ctx.exp_mul());

            char op = ctx.OP_MUL().getText().charAt(0);
            if (op == '%') {
                if (var1.getTipo() != Constants.INT); else if (var2.getTipo() == Constants.INT);
//  Error, se esperaba valor entero
                var3 = new Variable(Constants.INT, (Object) ((Integer) var1.getValor() % (Integer) var2.getValor()));
            } else {
                if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING);
//  Error, se esperaba valor numérico se encontró Constants.STRING
                var3 = new Variable(-1, null);

                if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                    if (op == '*') {
                        var3.setValor((Object) ((Double) var1.getValor() * (Double) var2.getValor()));
                    } else {
                        var3.setValor((Object) ((Double) var1.getValor() / (Double) var2.getValor()));
                    }
                    var3.setTipo(Constants.DOUBLE);
                } else {
                    if (op == '/') {
                        var3.setValor((Object) ((Integer) var1.getValor() * (Integer) var2.getValor()));
                    } else {
                        var3.setValor((Object) ((Integer) var1.getValor() / (Integer) var2.getValor()));
                    }
                    var3.setTipo(Constants.INT);
                }
            }
            return (T) var3;
        }
        return (T) var2;
    }

    @Override
    public T visitExp_pot(Exp_potContext ctx) {
        Variable var1, var2 = (Variable) visitExp_una(ctx.exp_una()), var3;
        if (ctx.exp_pot() != null) {
            var1 = (Variable) visitExp_pot(ctx.exp_pot());

            if (var1.getTipo() == Constants.STRING); else if (var2.getTipo() == Constants.STRING);
//  Error se esperaba valor numérico

            var3 = new Variable(-1, null);

            if (var1.getTipo() == Constants.DOUBLE || var2.getTipo() == Constants.DOUBLE) {
                var3.setValor((Object) Math.pow((Double) var1.getValor(), (Double) var2.getValor()));
                var3.setTipo(Constants.DOUBLE);
            } else {
                var3.setValor((Object) (Integer) (int) Math.pow((Integer) var1.getValor(), (Integer) var2.getValor()));
                var3.setTipo(Constants.INT);
            }
            return (T) var3;
        }
        return (T) var2;

    }

    @Override
    public T visitExp_una(Exp_unaContext ctx) {
        if (ctx.OP_UNA() != null) {
            char op = ctx.OP_UNA().getText().charAt(0);
            Variable var = (Variable) visitExp_una(ctx.exp_una());
            if (op == '!') {
                if (var.getTipo() != Constants.INT);
//  Error se esperaba valor entero
                var.setValor(((Integer) var.getValor() == 0) ? (Object) 1 : (Object) 0);
            } else {
                if (var.getTipo() == Constants.STRING);
//  Error se esperaba valor numérico se encontró Constants.STRING
                var.setValor((Object) ((Integer) var.getValor()));
            }
            return (T) var;
        } else {
            return visitTerm(ctx.term());
        }
    }

    String getTipo(int tipo) {
        switch (tipo) {
            case Constants.INT:
                return "INTEGER";
            case Constants.DOUBLE:
                return "DOUBLE";
            case Constants.STRING:
                return "STRING";
            default:
                return "Seras mamon";
        }
    }	
}