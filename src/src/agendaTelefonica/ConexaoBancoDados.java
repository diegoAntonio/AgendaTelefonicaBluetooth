// ConexaoBancoDados.java
// Fornece fácil conexão e criação de banco de dados UserContacts.
package src.agendaTelefonica;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class ConexaoBancoDados 
{
	//  Nome do banco
	private static final String DATABASE_NAME = "UserContacts";
	private SQLiteDatabase database; // Objeto de banco de dados
	private DatabaseOpenHelper databaseOpenHelper; // Banco de Dados Helper

	// Construtor público para DatabaseConnector
	public ConexaoBancoDados(Context context) 
	{
		// Cria um novo DatabaseOpenHelper
		databaseOpenHelper = 
				new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
	} // Fim de DatabaseConnector constructor

	// Abre a conexão com o banco
	public void open() throws SQLException 
	{
		// Criar ou abrir um banco de dados para leitura/escrita
		database = databaseOpenHelper.getWritableDatabase();
	} // Fim do metodo open

	// Fecha a conexão com o banco
	public void close() 
	{
		if (database != null)
			database.close(); // Fim do método close 
	} // Fim do metodo close

	// Insere um novo contato no banco de dados 
	public void insertContact(String name, String email, String phone, 
			String street, String city) 
	{
		ContentValues newContact = new ContentValues();
		newContact.put("name", name);
		newContact.put("email", email);
		newContact.put("phone", phone);
		newContact.put("street", street);
		newContact.put("city", city);

		open(); // Abrir a base de dados 
		database.insert("contacts", null, newContact);
		close(); // Fechar a base de dados
	} // Fim do metodo insertContact

	// Insere um novo contato no banco de dados 
	public void updateContact(long id, String name, String email, 
			String phone, String street, String city) 
	{
		ContentValues editContact = new ContentValues();
		editContact.put("name", name);
		editContact.put("email", email);
		editContact.put("phone", phone);
		editContact.put("street", street);
		editContact.put("city", city);

		open(); // Abrir a base de dados
		database.update("contacts", editContact, "_id=" + id, null);
		close(); // Fechar a base de dados
	} // Fim do metodo updateContact

	// Retorna um cursor com todas as informações de contato no banco de dados
	public Cursor getAllContacts() 
	{
		return database.query("contacts", new String[] {"_id", "name"}, 
				null, null, null, null, "name");
	} // Fim do metodo getAllContacts

	// Obtém um cursor que contém todas as informações sobre o contato especificado
	// Pelo id dado
	public Cursor getOneContact(long id) 
	{
		return database.query(
				"contacts", null, "_id=" + id, null, null, null, null);
	} // Fim do metodo getOnContact

	// Exclui o contato especificado pelo nome
	public void deleteContact(long id) 
	{
		open(); // Abrir a base de dados
		database.delete("contacts", "_id=" + id, null);
		close(); // Fechar a base de dados
	} // Fim do metodo deleteContact

	private class DatabaseOpenHelper extends SQLiteOpenHelper 
	{
		// Construtor público 
		public DatabaseOpenHelper(Context context, String name,
				CursorFactory factory, int version) 
		{
			super(context, name, factory, version);
		} // Fim de DatabaseOpenHelper constructor

		// Cria a tabela de contatos quando o banco de dados é criado 
		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			// Consulta para criar uma nova tabela chamada contatos 
			String createQuery = "CREATE TABLE contacts" +
					"(_id integer primary key autoincrement," +
					"name TEXT, email TEXT, phone TEXT," +
					"street TEXT, city TEXT);";

			db.execSQL(createQuery); // execute the query
		} // Fim do metodo onCreate

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) 
		{
		} // Fim do metodo onUpgrade
	} // Fim da classe DatabaseOpenHelper
} // Fim da classe DatabaseConnector